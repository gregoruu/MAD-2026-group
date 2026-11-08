package ee.movara.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.movies.MovieListsUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
  val movies: List<Movie> = emptyList(),
  val isLoading: Boolean = true,
  val error: String? = null
)

class HomeViewModel(
  private val repository: MovieRepository,
  lists: StateFlow<MovieListsUiState>
) : ViewModel() {
  private val remote = MutableStateFlow(HomeUiState())
  private var request: Job? = null
  val uiState = combine(remote, lists) { state, saved ->
    val watched = saved.lists.watched.map { it.id }.toSet()
    state.copy(movies = state.movies.distinctBy { it.id }.filterNot { it.id in watched })
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

  init { refresh() }

  fun refresh() {
    request?.cancel()
    request = viewModelScope.launch {
      remote.value = remote.value.copy(isLoading = true, error = null)
      remote.value = when (val result = repository.trendingMovies()) {
        is RepositoryResult.Success -> HomeUiState(result.data.movies, isLoading = false)
        is RepositoryResult.Error -> remote.value.copy(isLoading = false, error = result.message)
      }
    }
  }
}
