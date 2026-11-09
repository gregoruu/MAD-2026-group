package ee.movara.app.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.repository.MovieTasteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class PreferenceStartupState { LOADING, QUIZ, READY, ERROR }

/** Read the local profile before creating Home, avoiding a flash of the wrong destination. */
class PreferenceStartupViewModel(private val repository: MovieTasteRepository) : ViewModel() {
  private val state = MutableStateFlow(PreferenceStartupState.LOADING)
  val uiState = state.asStateFlow()
  private var loadJob: Job? = null

  init { retry() }

  fun retry() {
    loadJob?.cancel()
    state.value = PreferenceStartupState.LOADING
    loadJob = viewModelScope.launch {
      try {
        state.value = if (repository.profile.first().needsOnboarding) {
          PreferenceStartupState.QUIZ
        } else {
          PreferenceStartupState.READY
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        state.value = PreferenceStartupState.ERROR
      }
    }
  }

  /** Also allows entering Home for this session if local storage is unavailable. */
  fun enterApp() {
    loadJob?.cancel()
    state.value = PreferenceStartupState.READY
  }
}
