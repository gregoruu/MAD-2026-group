package ee.movara.app.ui.moviedetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.MovieDetails
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MovieDetailsUiState(
  val movie: MovieDetails? = null,
  val isLoading: Boolean = true,
  val error: String? = null
)

class MovieDetailsViewModel(
  private val repository: MovieRepository,
  private val movieId: Int
) : ViewModel() {
  private val mutableState = MutableStateFlow(MovieDetailsUiState())
  val uiState = mutableState.asStateFlow()
  private var request: Job? = null
  init { refresh() }

  fun refresh() {
    request?.cancel()
    request = viewModelScope.launch {
      mutableState.value = mutableState.value.copy(isLoading = true, error = null)
      mutableState.value = when (val result = repository.movieDetails(movieId)) {
        is RepositoryResult.Success -> MovieDetailsUiState(result.data, isLoading = false)
        is RepositoryResult.Error -> mutableState.value.copy(isLoading = false, error = result.message)
      }
    }
  }
}
