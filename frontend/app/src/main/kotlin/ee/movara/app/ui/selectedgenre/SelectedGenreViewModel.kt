
package ee.movara.app.ui.selectedgenre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ee.movara.app.ui.state.UiState
import ee.movara.app.ui.state.listUiState

//UI state for the Selected Genre screen


data class SelectedGenreUiState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreError: String? = null,
    val hasMore: Boolean = false
) {
    val contentState: UiState<List<Movie>>
        get() = listUiState(
            items = movies,
            loading = isLoading,
            errorMessage = errorMessage
        )
}


//Loads movies belonging to the selected TMDB genre
//Uses MovieRepository rather than calling the API directly
class SelectedGenreViewModel(
    private val genreId: Int,
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectedGenreUiState())
    val uiState: StateFlow<SelectedGenreUiState> =
        _uiState.asStateFlow()

    private var nextPage = 1
    private var requestInProgress = false

    init {
        loadFirstPage()
    }

    fun loadFirstPage() {
        if (requestInProgress) return

        nextPage = 1

        _uiState.value = SelectedGenreUiState(
            isLoading = true
        )

        requestPage(page = 1)
    }

    fun loadMore() {
        val state = _uiState.value

        if (
            requestInProgress ||
            state.isLoading ||
            state.isLoadingMore ||
            !state.hasMore ||
            state.loadMoreError != null
        ) {
            return
        }

        _uiState.update {
            it.copy(isLoadingMore = true)
        }

        requestPage(page = nextPage)
    }

    fun retry() {
        if (requestInProgress) return

        if (_uiState.value.movies.isEmpty()) {
            loadFirstPage()
        } else {
            _uiState.update {
                it.copy(loadMoreError = null)
            }
            loadMore()
        }
    }

    private fun requestPage(page: Int) {
        if (requestInProgress) return

        requestInProgress = true

        viewModelScope.launch {
            try {
                when (
                    val result = movieRepository.moviesInGenre(
                        genreId = genreId,
                        page = page
                    )
                ) {
                    is RepositoryResult.Success -> {
                        val moviePage = result.data

                        nextPage = moviePage.page + 1

                        _uiState.update { current ->
                            val updatedMovies = if (page == 1) {
                                moviePage.movies
                            } else {
                                (current.movies + moviePage.movies)
                                    .distinctBy { it.id }
                            }

                            current.copy(
                                movies = updatedMovies,
                                isLoading = false,
                                isLoadingMore = false,
                                errorMessage = null,
                                loadMoreError = null,
                                hasMore =
                                    moviePage.page < moviePage.totalPages
                            )
                        }
                    }

                    is RepositoryResult.Error -> {
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                isLoadingMore = false,
                                errorMessage = if (page == 1) {
                                    result.message
                                } else {
                                    null
                                },
                                loadMoreError = if (page > 1) {
                                    result.message
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            } finally {
                requestInProgress = false
            }
        }
    }
}

class SelectedGenreViewModelFactory(
    private val genreId: Int,
    private val movieRepository: MovieRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        require(
            modelClass.isAssignableFrom(
                SelectedGenreViewModel::class.java
            )
        ) {
            "Unknown ViewModel class: ${modelClass.name}"
        }

        return SelectedGenreViewModel(
            genreId = genreId,
            movieRepository = movieRepository
        ) as T
    }
}
