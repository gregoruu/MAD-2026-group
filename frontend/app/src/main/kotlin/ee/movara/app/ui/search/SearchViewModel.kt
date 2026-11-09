package ee.movara.app.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ee.movara.app.ui.state.UiState
import ee.movara.app.ui.state.listUiState


data class SearchUiState(
  val query: String = "",
  val movies: List<Movie> = emptyList(),
  val isLoading: Boolean = false,
  val showTitleError: Boolean = false,
  val requestError: String? = null,
  val page: Int = 0,
  val totalPages: Int = 0
) {
  val canLoadMore: Boolean
    get() = page > 0 && page < totalPages

  // Keep existing movies visible while loading another page.
  val contentState: UiState<List<Movie>>
    get() = listUiState(
      items = movies,
      loading = isLoading,
      errorMessage = requestError
    )
}
class SearchViewModel(
  private val repository: MovieRepository,
  private val savedStateHandle: SavedStateHandle
) : ViewModel() {

  private val restoredQuery: String =
    savedStateHandle["searchQuery"] ?: ""

  private val mutableUiState = MutableStateFlow(
    SearchUiState(
      query = restoredQuery,
      isLoading = restoredQuery.isNotBlank()
    )
  )

  val uiState = mutableUiState.asStateFlow()
  private var searchJob: Job? = null

  init {
    if (uiState.value.query.isNotBlank()) requestPage(1, debounce = true)
  }


  fun onQueryChange(query: String) {
    if (query == uiState.value.query) return

    searchJob?.cancel()
    savedStateHandle["searchQuery"] = query

    mutableUiState.value = SearchUiState(
      query = query,
      isLoading = query.isNotBlank()
    )

    if (query.isNotBlank()) {
      requestPage(1, debounce = true)
    }
  }


  fun submitSearch() {
    if (uiState.value.query.isBlank()) {
      searchJob?.cancel()
      mutableUiState.value = SearchUiState(
        query = uiState.value.query,
        showTitleError = true
      )
      return
    }
    // Submitting an already displayed query does not repeat the same request.
    if (uiState.value.page > 0 && uiState.value.requestError == null) return
    requestPage(1)
  }

  fun loadMore() {
    val state = uiState.value
    if (state.canLoadMore && !state.isLoading && state.requestError == null) {
      requestPage(state.page + 1)
    }
  }

  fun retry() {
    if (uiState.value.requestError != null && !uiState.value.isLoading) {
      requestPage(uiState.value.page + 1)
    }
  }


  private fun requestPage(
    page: Int,
    debounce: Boolean = false
  ) {
    searchJob?.cancel()

    val title = uiState.value.query.trim()
    if (title.isEmpty()) return

    // Show loading immediately, before the coroutine starts.
    mutableUiState.update {
      it.copy(
        isLoading = true,
        requestError = null,
        showTitleError = false
      )
    }

    searchJob = viewModelScope.launch {
      if (debounce) delay(400)

      val result = repository.searchMovies(title, page)
      // Even a source that finishes after cancellation cannot restore stale results.
      currentCoroutineContext().ensureActive()
      when (result) {
        is RepositoryResult.Success -> mutableUiState.update {
          it.copy(
            movies = (if (page == 1) result.data.movies.take(20)
              else it.movies + result.data.movies.take(20)).distinctBy(Movie::id),
            page = result.data.page,
            totalPages = result.data.totalPages,
            isLoading = false
          )
        }
        is RepositoryResult.Error -> mutableUiState.update {
          it.copy(isLoading = false, requestError = result.message)
        }
      }
    }
  }
}
