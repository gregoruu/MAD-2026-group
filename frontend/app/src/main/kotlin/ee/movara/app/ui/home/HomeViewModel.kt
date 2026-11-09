package ee.movara.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.HomeRecommendationLoader
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.MovieTasteRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieTastePreferences
import ee.movara.app.domain.model.MovieTasteProfile
import ee.movara.app.domain.recommendation.MovieRecommendation
import ee.movara.app.domain.recommendation.MovieRecommendations
import ee.movara.app.domain.recommendation.RecommendationBatch
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
  val loading: Boolean = true,
  val recommendations: List<MovieRecommendation> = emptyList(),
  val likedIds: Set<Int> = emptySet(),
  val watchLaterIds: Set<Int> = emptySet(),
  val loadFailed: Boolean = false,
  val usingCache: Boolean = false,
  val actionFailed: Boolean = false,
  val minimumRating: Int? = null
) {
  val topRecommendation: MovieRecommendation? get() = recommendations.firstOrNull()
  val topPick: Movie? get() = topRecommendation?.movie
}

private data class HomeData(
  val loading: Boolean = true,
  val preferences: MovieTastePreferences = MovieTastePreferences(),
  val likedIds: Set<Int> = emptySet(),
  val watchLaterIds: Set<Int> = emptySet(),
  val batches: List<RecommendationBatch> = emptyList(),
  val loadFailed: Boolean = false,
  val usingCache: Boolean = false,
  val actionFailed: Boolean = false,
  val today: LocalDate = LocalDate.MIN
)

private data class HomeInput(
  val preferences: MovieTastePreferences?,
  val likedMovies: List<Movie>?
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
  private val repository: MovieRepository,
  private val tasteRepository: MovieTasteRepository,
  private val today: () -> LocalDate = { LocalDate.now(ZoneOffset.UTC) }
) : ViewModel() {
  private val state = MutableStateFlow(HomeData())
  private val loader = HomeRecommendationLoader(repository)
  private val refresh = MutableStateFlow(0)
  private val pendingActions = mutableSetOf<Pair<Int, MovieListType>>()

  // Keep all fetched candidates. Watched changes rerank immediately without extra HTTP requests.
  val uiState = combine(state, refresh.flatMapLatest { repository.observeWatchedMovies() }) { current, watched ->
    val watchedIds = (watched as? RepositoryResult.Success)?.data?.map(Movie::id)?.toSet()
    HomeUiState(
      loading = current.loading,
      recommendations = if (watchedIds == null) emptyList() else MovieRecommendations.rank(
        current.batches, current.preferences, current.likedIds, watchedIds, current.today
      ),
      likedIds = current.likedIds,
      watchLaterIds = current.watchLaterIds,
      loadFailed = current.loadFailed || watchedIds == null,
      usingCache = current.usingCache,
      actionFailed = current.actionFailed,
      minimumRating = current.preferences.minimumRating
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

  init {
    viewModelScope.launch {
      refresh.flatMapLatest { repository.observeWatchLaterMovies() }.collect { result ->
        when (result) {
          is RepositoryResult.Success -> state.update {
            it.copy(watchLaterIds = result.data.map(Movie::id).toSet())
          }
          is RepositoryResult.Error -> state.update { it.copy(actionFailed = true) }
        }
      }
    }
    viewModelScope.launch {
      refresh.flatMapLatest {
        // Retry resubscribes after a failed DataStore read instead of substituting default preferences.
        val preferences = tasteRepository.profile.map<MovieTasteProfile, MovieTastePreferences?> {
          it.preferences
        }.catch { failure ->
          if (failure is CancellationException) throw failure
          emit(null)
        }
        combine(preferences, repository.observeLikedMovies()) { taste, liked ->
          HomeInput(taste, (liked as? RepositoryResult.Success)?.data)
        }.distinctUntilChanged()
      }.collectLatest { input ->
        val preferences = input.preferences
        val likes = input.likedMovies
        if (preferences == null || likes == null) {
          state.update { it.copy(loading = false, batches = emptyList(), loadFailed = true) }
          return@collectLatest
        }
        val date = today()
        state.update { it.copy(
          loading = true, loadFailed = false, usingCache = false, batches = emptyList(),
          preferences = preferences, likedIds = likes.map(Movie::id).toSet(), today = date
        ) }
        val result = loader.load(preferences, likes, date)
        currentCoroutineContext().ensureActive()
        state.update { it.copy(
          loading = false, batches = result.batches,
          loadFailed = result.hasFailures, usingCache = result.usingCache
        ) }
      }
    }
  }

  fun retry() { refresh.value += 1 }

  fun toggleLike(movie: Movie) = toggle(movie, MovieListType.LIKED)
  fun toggleWatchLater(movie: Movie) = toggle(movie, MovieListType.WATCH_LATER)

  fun dismissActionError() { state.update { it.copy(actionFailed = false) } }

  private fun toggle(movie: Movie, listType: MovieListType) {
    val key = movie.id to listType
    if (!pendingActions.add(key)) return
    viewModelScope.launch {
      try {
        val ids = if (listType == MovieListType.LIKED) state.value.likedIds else state.value.watchLaterIds
        val result = if (movie.id in ids) {
          repository.removeMovie(movie.id, listType)
        } else {
          repository.saveMovie(movie, listType)
        }
        if (result is RepositoryResult.Error) state.update { it.copy(actionFailed = true) }
      } finally {
        pendingActions.remove(key)
      }
    }
  }
}
