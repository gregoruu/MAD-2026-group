
package ee.movara.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.repository.DataRepository
import ee.movara.app.ui.state.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel(
  dataRepository: DataRepository
) : ViewModel() {

  val uiState: StateFlow<UiState<List<String>>> =
    dataRepository.data
      .map<List<String>, UiState<List<String>>> { data ->
        if (data.isEmpty()) {
          UiState.Empty
        } else {
          UiState.Success(data)
        }
      }
      .catch { error ->
        emit(
          UiState.Error(
            message = error.message ?: "Unable to load data",
            cause = error
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
      )
}
