package ee.movara.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MoviePage
import ee.movara.app.ui.movies.MovieListsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
  val storageError: String? = null,
  val actionsEnabled: Boolean = true,
  val pendingIds: Set<Int> = emptySet()
) {
  val topPick: Movie? get() = rails.firstNotNullOfOrNull { it.movies.firstOrNull() }
}

/** Recommendations use recent likes; all membership comes from the shared Room snapshot. */
class HomeViewModel(
  private val repository: MovieRepository,
  private val movieLists: MovieListsStore
) : ViewModel() {
  private val state = MutableStateFlow(HomeUiState())
  private val refresh = MutableStateFlow(0)

  // Retain fetched rails so Watched changes only refilter, without another TMDB request.
  val uiState = combine(state, movieLists.uiState) { current, saved ->
    val watchedIds = saved.lists.watched.map(Movie::id).toSet()
    current.copy(
      loading = saved.storageError == null && (current.loading || saved.isLoading),
      rails = if (saved.isLoading || saved.storageError != null) emptyList() else {
        current.rails.map { rail ->
          rail.copy(movies = rail.movies.filterNot { it.id in watchedIds })
        }
      },
      likedIds = saved.lists.liked.map(Movie::id).toSet(),
      watchLaterIds = saved.lists.watchLater.map(Movie::id).toSet(),
      loadFailed = current.loadFailed || saved.storageError != null,
      storageError = saved.storageError,
      actionsEnabled = saved.actionsEnabled,
      pendingIds = saved.pendingIds
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

  init {
    viewModelScope.launch {
      val sources = movieLists.uiState
        .filter { !it.isLoading && it.storageError == null }
        // Room supplies the most recently added movies first.
        .map { it.lists.liked.take(5) }
        .distinctUntilChanged()
      combine(sources, refresh) { liked, _ -> liked }.collectLatest { liked ->
        state.update { it.copy(loading = true, loadFailed = false) }
        val rails = if (liked.isEmpty()) {
          listOf(toRail(null, repository.trendingMovies()))
        } else {
          liked.map { movie -> toRail(movie, repository.recommendedMovies(movie.id)) }
        }
        state.update { it.copy(loading = false, rails = rails) }
      }
    }
  }

  fun retry() {
    if (movieLists.uiState.value.storageError != null) movieLists.retry()
    refresh.value += 1
  }

  fun toggleLike(movie: Movie) = toggle(movie, MovieListType.LIKED)
  fun toggleWatchLater(movie: Movie) = toggle(movie, MovieListType.WATCH_LATER)

  private fun toggle(movie: Movie, type: MovieListType) {
    movieLists.setMembership(movie, type, !movieLists.uiState.value.lists.contains(movie.id, type))
  }

  private fun toRail(source: Movie?, result: RepositoryResult<MoviePage>): HomeRail = when (result) {
    is RepositoryResult.Success -> HomeRail(source, result.data.movies.distinctBy(Movie::id))
    is RepositoryResult.Error -> HomeRail(sourceMovie = source, failed = true)
  }
}
