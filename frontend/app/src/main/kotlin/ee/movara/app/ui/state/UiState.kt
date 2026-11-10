
package ee.movara.app.ui.state

// Shared state for screens that load data asynchronously
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Success<out T>(
        val data: T
    ) : UiState<T>

    data object Empty : UiState<Nothing>

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : UiState<Nothing>
}

// Keep existing results visible while loading another page
fun <T> listUiState(
    items: List<T>,
    loading: Boolean,
    errorMessage: String? = null
): UiState<List<T>> = when {
    items.isNotEmpty() -> UiState.Success(items)
    loading -> UiState.Loading
    errorMessage != null -> UiState.Error(errorMessage)
    else -> UiState.Empty
}
