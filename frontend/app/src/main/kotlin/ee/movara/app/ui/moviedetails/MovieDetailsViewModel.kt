
package ee.movara.app.ui.moviedetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieDetails
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MovieDetailsUiState(
    val movie: MovieDetails? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isLiked: Boolean = false,
    val isWatchLater: Boolean = false,
    val isWatched: Boolean = false,
    val actionError: String? = null
)

class MovieDetailsViewModel(
    private val movieId: Int,
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailsUiState())
    val uiState: StateFlow<MovieDetailsUiState> =
        _uiState.asStateFlow()

    private var loadJob: Job? = null
    private val actionMutex = Mutex()

    init {
        MovieListType.entries.forEach { type ->
            viewModelScope.launch {
                repository.observeMembership(movieId, type)
                    .collect { result ->
                        when (result) {
                            is RepositoryResult.Success -> {
                                _uiState.update { current ->
                                    when (type) {
                                        MovieListType.LIKED ->
                                            current.copy(isLiked = result.data)
                                        MovieListType.WATCH_LATER ->
                                            current.copy(isWatchLater = result.data)
                                        MovieListType.WATCHED ->
                                            current.copy(isWatched = result.data)
                                    }
                                }
                            }

                            is RepositoryResult.Error -> {
                                _uiState.update {
                                    it.copy(actionError = result.message)
                                }
                            }
                        }
                    }
            }
        }

        loadMovie()
    }

    fun loadMovie() {
        loadJob?.cancel()

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        loadJob = viewModelScope.launch {
            when (val result = repository.movieDetails(movieId)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            movie = result.data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }

                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun toggleLike() = toggle(MovieListType.LIKED)

    fun toggleWatchLater() = toggle(MovieListType.WATCH_LATER)

    fun toggleWatched() = toggle(MovieListType.WATCHED)


    private fun toggle(type: MovieListType) {
        viewModelScope.launch {
            actionMutex.withLock {
                val details = _uiState.value.movie
                    ?: return@withLock

                // Read the current state when this action starts,
                // not when it is first queued.
                val selected = when (type) {
                    MovieListType.LIKED -> _uiState.value.isLiked
                    MovieListType.WATCH_LATER -> _uiState.value.isWatchLater
                    MovieListType.WATCHED -> _uiState.value.isWatched
                }

                val movie = Movie(
                    id = details.id,
                    title = details.title,
                    posterPath = details.posterPath,
                    releaseYear = details.releaseYear
                )

                val result = if (selected) {
                    repository.removeMovie(movie.id, type)
                } else {
                    repository.saveMovie(movie, type)
                }

                when (result) {
                    is RepositoryResult.Error -> {
                        _uiState.update {
                            it.copy(actionError = result.message)
                        }
                    }

                    is RepositoryResult.Success -> {
                        // Update immediately so the next queued action
                        // observes the latest selected state.
                        _uiState.update { current ->
                            when (type) {
                                MovieListType.LIKED ->
                                    current.copy(isLiked = !selected)

                                MovieListType.WATCH_LATER ->
                                    current.copy(isWatchLater = !selected)

                                MovieListType.WATCHED ->
                                    current.copy(isWatched = !selected)
                            }
                        }

                        // Preserve the existing rule:
                        // marking Watched also removes Watch Later.
                        if (
                            type == MovieListType.WATCHED &&
                            !selected
                        ) {
                            when (
                                val removal = repository.removeMovie(
                                    movie.id,
                                    MovieListType.WATCH_LATER
                                )
                            ) {
                                is RepositoryResult.Success -> {
                                    _uiState.update {
                                        it.copy(isWatchLater = false)
                                    }
                                }

                                is RepositoryResult.Error -> {
                                    _uiState.update {
                                        it.copy(
                                            actionError = removal.message
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    fun clearActionError() {
        _uiState.update { it.copy(actionError = null) }
    }
}
