package ee.movara.app.ui.movies

import kotlinx.coroutines.CoroutineScope
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieLists
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
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

/** Feedback is emitted only after a write fails or Room confirms the new membership. */
data class MovieListFeedback(
  val movie: Movie,
  val type: MovieListType,
  val selected: Boolean,
  val error: String? = null
)

/** Room emissions, rather than optimistic copies, are the source of membership truth. */
class MovieListsStore(private val repository: MovieRepository, private val scope: CoroutineScope) {
  private val mutableState = MutableStateFlow(MovieListsUiState())
  val uiState = mutableState.asStateFlow()
  private val feedbackChannel = Channel<MovieListFeedback>(Channel.UNLIMITED)
  val feedback = feedbackChannel.receiveAsFlow()
  private var observer: Job? = null

  init { retry() }

  fun retry() {
    observer?.cancel()
    mutableState.update { it.copy(isLoading = true, storageError = null) }
    observer = scope.launch {
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
    scope.launch {
      var feedback: MovieListFeedback? = null
      try {
        val result = if (selected) repository.saveMovie(movie, type) else repository.removeMovie(movie.id, type)
        when (result) {
          is RepositoryResult.Error -> feedback = MovieListFeedback(movie, type, selected, result.message)
          is RepositoryResult.Success -> {
            val confirmed = uiState.first {
              it.storageError != null ||
                (it.lists.contains(movie.id, type) == selected &&
                  (type != MovieListType.WATCHED || !selected ||
                    !it.lists.contains(movie.id, MovieListType.WATCH_LATER)))
            }
            if (confirmed.storageError == null) feedback = MovieListFeedback(movie, type, selected)
          }
        }
      } finally {
        mutableState.update { it.copy(pendingIds = it.pendingIds - movie.id) }
      }
      feedback?.let { feedbackChannel.send(it) }
    }
  }
}
