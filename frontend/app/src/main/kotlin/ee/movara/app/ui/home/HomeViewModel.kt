package ee.movara.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ee.movara.app.ui.state.UiState

data class HomeRail(
  val sourceMovie: Movie? = null,
  val movies: List<Movie> = emptyList(),
  val failed: Boolean = false
)

data class HomeUiState(
  val loading: Boolean = true,
  val rails: List<HomeRail> = emptyList(),
  val likedIds: Set<Int> = emptySet(),
  val watchLaterIds: Set<Int> = emptySet(),
  val loadFailed: Boolean = false,
  val actionFailed: Boolean = false
) {

  val topPick: Movie?
    get() = rails.firstNotNullOfOrNull {
      it.movies.firstOrNull()
    }

  val contentState: UiState<List<HomeRail>>
    get() = when {
      loading -> UiState.Loading

      loadFailed -> UiState.Error(
        "Could not load recommendations"
      )

      rails.isEmpty() -> UiState.Empty

      else -> UiState.Success(rails)
    }
}


/** Home presentation; recommendation ranking and filtering can evolve independently of the UI. */
class HomeViewModel(private val repository: MovieRepository) : ViewModel() {
  private val state = MutableStateFlow(HomeUiState())
  // Keep the fetched rails intact so watched-list changes can refilter locally.
  val uiState = combine(state, repository.observeWatchedMovies()) { current, watched ->
    when (watched) {
      is RepositoryResult.Success -> {
        val watchedIds = watched.data.map(Movie::id).toSet()
        current.copy(rails = current.rails.map { rail ->
          rail.copy(movies = rail.movies.filterNot { it.id in watchedIds })
        })
      }
      is RepositoryResult.Error ->
        current.copy(loading = false, rails = emptyList(), loadFailed = true)
    }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())
  private val refresh = MutableStateFlow(0)
  private val pendingActions = mutableSetOf<Pair<Int, MovieListType>>()

  init {
    viewModelScope.launch {
      repository.observeWatchLaterMovies().collect { result ->
        when (result) {
          is RepositoryResult.Success -> state.update {
            it.copy(watchLaterIds = result.data.map(Movie::id).toSet())
          }
          is RepositoryResult.Error -> state.update { it.copy(actionFailed = true) }
        }
      }
    }
    viewModelScope.launch {
      val likedMovies = repository.observeLikedMovies().map { result ->
        if (result is RepositoryResult.Success) {
          state.update { it.copy(likedIds = result.data.map(Movie::id).toSet()) }
          // Room supplies the most recently added movies first.
          RepositoryResult.Success(result.data.take(5), result.source)
        } else {
          result
        }
      }.distinctUntilChanged()
      combine(likedMovies, refresh) { liked, _ -> liked }.collectLatest { liked ->
        state.update { it.copy(loading = true, loadFailed = false) }
        if (liked is RepositoryResult.Error) {
          state.update { it.copy(loading = false, rails = emptyList(), loadFailed = true) }
          return@collectLatest
        }
        val sources = (liked as RepositoryResult.Success).data
        val rails = if (sources.isEmpty()) {
          listOf(toRail(null, repository.trendingMovies()))
        } else {
          sources.map { movie -> toRail(movie, repository.recommendedMovies(movie.id)) }
        }
        state.update { it.copy(loading = false, rails = rails) }
      }
    }
  }

  fun retry() {
    refresh.value += 1
  }

  fun toggleLike(movie: Movie) = toggle(movie, MovieListType.LIKED)
  fun toggleWatchLater(movie: Movie) = toggle(movie, MovieListType.WATCH_LATER)

  fun dismissActionError() {
    state.update { it.copy(actionFailed = false) }
  }

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

  private fun toRail(
    source: Movie?,
    result: RepositoryResult<ee.movara.app.domain.model.MoviePage>
  ): HomeRail = when (result) {
    is RepositoryResult.Success -> HomeRail(source, result.data.movies.distinctBy(Movie::id))
    is RepositoryResult.Error -> HomeRail(sourceMovie = source, failed = true)
  }
}
