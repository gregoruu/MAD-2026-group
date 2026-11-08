package ee.movara.app.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieLists
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovieListsUiState(
  val lists: MovieLists = MovieLists(),
  val isLoading: Boolean = true,
  val storageError: String? = null,
  val pendingIds: Set<Int> = emptySet()
) {
  val actionsEnabled get() = !isLoading && storageError == null
}

/** Room emissions, rather than optimistic copies, are the source of membership truth. */
class MovieListsViewModel(private val repository: MovieRepository) : ViewModel() {
  private val mutableState = MutableStateFlow(MovieListsUiState())
  val uiState = mutableState.asStateFlow()
  private var observer: Job? = null

  init { retry() }

  fun retry() {
    observer?.cancel()
    mutableState.update { it.copy(isLoading = true, storageError = null) }
    observer = viewModelScope.launch {
      repository.observeLists().collect { result ->
        mutableState.update { state ->
          when (result) {
            is RepositoryResult.Success -> state.copy(lists = result.data, isLoading = false, storageError = null)
            is RepositoryResult.Error -> state.copy(isLoading = false, storageError = result.message)
          }
        }
      }
    }
  }

  fun setMembership(movie: Movie, type: MovieListType, selected: Boolean) {
    val state = uiState.value
    if (!state.actionsEnabled || movie.id in state.pendingIds) return
    mutableState.update { it.copy(pendingIds = it.pendingIds + movie.id) }
    viewModelScope.launch {
      try {
        val result = if (selected) repository.saveMovie(movie, type) else repository.removeMovie(movie.id, type)
        when (result) {
          is RepositoryResult.Error -> mutableState.update { it.copy(storageError = result.message) }
          is RepositoryResult.Success -> uiState.first {
            it.storageError != null ||
              (it.lists.contains(movie.id, type) == selected &&
                (type != MovieListType.WATCHED || !selected ||
                  !it.lists.contains(movie.id, MovieListType.WATCH_LATER)))
          }
        }
      } finally {
        mutableState.update { it.copy(pendingIds = it.pendingIds - movie.id) }
      }
    }
  }
}
