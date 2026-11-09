package ee.movara.app.ui.moviedetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieDetails
import ee.movara.app.ui.movies.MovieListsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovieDetailsUiState(
    val movie: MovieDetails? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isLiked: Boolean = false,
    val isWatchLater: Boolean = false,
    val isWatched: Boolean = false,
    val actionError: String? = null,
    val actionsEnabled: Boolean = false
)

class MovieDetailsViewModel(
    private val movieId: Int,
    private val repository: MovieRepository,
    private val movieLists: MovieListsViewModel
) : ViewModel() {
    private val state = MutableStateFlow(MovieDetailsUiState())
    val uiState = combine(state, movieLists.uiState) { current, saved ->
        current.copy(
            isLiked = saved.lists.contains(movieId, MovieListType.LIKED),
            isWatchLater = saved.lists.contains(movieId, MovieListType.WATCH_LATER),
            isWatched = saved.lists.contains(movieId, MovieListType.WATCHED),
            actionError = saved.storageError,
            actionsEnabled = saved.actionsEnabled && movieId !in saved.pendingIds
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MovieDetailsUiState())

    private var loadJob: Job? = null

    init { loadMovie() }

    fun loadMovie() {
        loadJob?.cancel()
        state.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            when (val result = repository.movieDetails(movieId)) {
                is RepositoryResult.Success -> state.update {
                    it.copy(movie = result.data, isLoading = false, errorMessage = null)
                }
                is RepositoryResult.Error -> state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun toggleLike() = toggle(MovieListType.LIKED)
    fun toggleWatchLater() = toggle(MovieListType.WATCH_LATER)
    fun toggleWatched() = toggle(MovieListType.WATCHED)
    fun retryLists() = movieLists.retry()

    private fun toggle(type: MovieListType) {
        val details = state.value.movie ?: return
        val movie = Movie(details.id, details.title, details.posterPath, details.releaseYear)
        movieLists.setMembership(movie, type, !movieLists.uiState.value.lists.contains(movie.id, type))
    }
}
