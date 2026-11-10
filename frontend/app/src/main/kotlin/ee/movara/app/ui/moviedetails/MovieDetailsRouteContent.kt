
package ee.movara.app.ui.moviedetails

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ee.movara.app.ui.components.PulsingPlaceholder
import ee.movara.app.ui.theme.MovaraBackground

@Composable
fun MovieDetailsRouteContent(
    viewModel: MovieDetailsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(containerColor = MovaraBackground) { padding ->

        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }

                    PulsingPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        cornerRadius = 24.dp
                    )

                    PulsingPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                    )

                    PulsingPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(20.dp)
                    )

                    PulsingPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                }
            }

            state.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.errorMessage.orEmpty()
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = viewModel::loadMovie
                    ) {
                        Text("Try again")
                    }

                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            }

            state.movie != null -> {
                val movie = requireNotNull(state.movie)

                Column(Modifier.padding(padding)) {
                    state.actionError?.let { message ->
                        Text(message, Modifier.padding(horizontal = 20.dp))
                        TextButton(onClick = viewModel::retryLists) { Text("Retry storage") }
                    }
                    MovieDetailsScreen(
                        movie = movie,
                        isLiked = state.isLiked,
                        isWatchLater = state.isWatchLater,
                        isWatched = state.isWatched,
                        onLikeClick = viewModel::toggleLike,
                        onWatchLaterClick = viewModel::toggleWatchLater,
                        onWatchedClick = viewModel::toggleWatched,
                        onBack = onBack,
                        actionsEnabled = state.actionsEnabled
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Movie details unavailable")
                }
            }
        }
    }
}
