package ee.movara.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.HomeRecommendationLoader
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.MovieTasteRepository
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.movies.MovieListsStore
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ee.movara.app.ui.state.UiState

data class HomeUiState(
  val loading: Boolean = true,
  val recommendations: List<MovieRecommendation> = emptyList(),
  val likedIds: Set<Int> = emptySet(),
  val watchLaterIds: Set<Int> = emptySet(),
  val loadFailed: Boolean = false,
  val usingCache: Boolean = false,
  val minimumRating: Int? = null,
  val storageError: String? = null,
  val actionsEnabled: Boolean = true,
  val pendingIds: Set<Int> = emptySet()
) {
  val topRecommendation: MovieRecommendation? get() = recommendations.firstOrNull()
  val topPick: Movie? get() = topRecommendation?.movie

  val contentState: UiState<List<MovieRecommendation>>
    get() = when {
      loading -> UiState.Loading
      // A failed source must not hide successful recommendations from other sources.
      recommendations.isNotEmpty() -> UiState.Success(recommendations)
      loadFailed -> UiState.Error("Could not load recommendations")
      else -> UiState.Empty
    }
}

private data class HomeData(
  val loading: Boolean = true,
  val preferences: MovieTastePreferences = MovieTastePreferences(),
  val batches: List<RecommendationBatch> = emptyList(),
  val loadFailed: Boolean = false,
  val usingCache: Boolean = false,
  val today: LocalDate = LocalDate.MIN
)

private data class HomeInput(
  val preferences: MovieTastePreferences?,
  val likedMovies: List<Movie>?,
  val refreshVersion: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
  repository: MovieRepository,
  private val tasteRepository: MovieTasteRepository,
  private val movieLists: MovieListsStore,
  private val today: () -> LocalDate = { LocalDate.now(ZoneOffset.UTC) }
) : ViewModel() {
  private val state = MutableStateFlow(HomeData())
  private val loader = HomeRecommendationLoader(repository)
  private val refresh = MutableStateFlow(0)
  private var completedRefresh = 0

  // Keep all fetched candidates. Watched changes rerank immediately without extra HTTP requests.
  val uiState = combine(state, movieLists.uiState) { current, saved ->
    val watchedIds = saved.lists.watched.map(Movie::id).toSet()
    HomeUiState(
      loading = saved.storageError == null && (current.loading || saved.isLoading),
      recommendations = if (saved.isLoading || saved.storageError != null) emptyList() else MovieRecommendations.rank(
        current.batches, current.preferences, saved.lists.liked.map(Movie::id).toSet(), watchedIds, current.today
      ),
      likedIds = saved.lists.liked.map(Movie::id).toSet(),
      watchLaterIds = saved.lists.watchLater.map(Movie::id).toSet(),
      loadFailed = current.loadFailed || saved.storageError != null,
      usingCache = current.usingCache,
      minimumRating = current.preferences.minimumRating,
      storageError = saved.storageError,
      actionsEnabled = saved.actionsEnabled,
      pendingIds = saved.pendingIds
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

  init {
    viewModelScope.launch {
      refresh.flatMapLatest { refreshVersion ->
        // Retry resubscribes after a failed DataStore read instead of substituting default preferences.
        val preferences = tasteRepository.profile.map<MovieTasteProfile, MovieTastePreferences?> {
          it.preferences
        }.catch { failure ->
          if (failure is CancellationException) throw failure
          emit(null)
        }
        combine(preferences, movieLists.uiState
          .filter { !it.isLoading && it.storageError == null }
          .map { it.lists.liked }
          .distinctUntilChanged()) { taste, liked ->
          HomeInput(taste, liked, refreshVersion)
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
          preferences = preferences, today = date
        ) }
        val result = loader.load(
          preferences, likes, date, forceRefresh = input.refreshVersion != completedRefresh
        )
        currentCoroutineContext().ensureActive()
        completedRefresh = input.refreshVersion
        state.update { it.copy(
          loading = false, batches = result.batches,
          loadFailed = result.hasFailures, usingCache = result.usingCache
        ) }
      }
    }
  }

  fun retry() {
    if (movieLists.uiState.value.storageError != null) movieLists.retry()
    refresh.value += 1
  }

  fun toggleLike(movie: Movie) = toggle(movie, MovieListType.LIKED)
  fun toggleWatchLater(movie: Movie) = toggle(movie, MovieListType.WATCH_LATER)

  private fun toggle(movie: Movie, listType: MovieListType) {
    movieLists.setMembership(movie, listType, !movieLists.uiState.value.lists.contains(movie.id, listType))
  }
}
