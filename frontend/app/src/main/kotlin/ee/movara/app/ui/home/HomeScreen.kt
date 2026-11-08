package ee.movara.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ee.movara.app.R
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.components.MovieAction
import ee.movara.app.ui.components.MovieActionButton
import ee.movara.app.ui.components.MovieCard
import ee.movara.app.ui.components.PulsingPlaceholder
import ee.movara.app.ui.components.SpotlightCard
import ee.movara.app.ui.theme.MovaraTheme

@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  onSearch: () -> Unit,
  onMovieClick: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbar = remember { SnackbarHostState() }
  val actionError = stringResource(R.string.home_action_error)
  LaunchedEffect(state.actionFailed) {
    if (state.actionFailed) {
      snackbar.showSnackbar(actionError)
      viewModel.dismissActionError()
    }
  }
  androidx.compose.foundation.layout.Box(modifier.fillMaxSize()) {
    HomeContent(
      state = state,
      onSearch = onSearch,
      onMovieClick = onMovieClick,
      onLike = viewModel::toggleLike,
      onWatchLater = viewModel::toggleWatchLater,
      onRetry = viewModel::retry
    )
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
  }
}

@Composable
internal fun HomeContent(
  state: HomeUiState,
  onSearch: () -> Unit,
  onMovieClick: (Int) -> Unit,
  onLike: (Movie) -> Unit,
  onWatchLater: (Movie) -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier
) {
  val spotlightHeight = LocalConfiguration.current.screenHeightDp.dp * 0.62f
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(40.dp)
  ) {
    item(key = "header") {
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          stringResource(R.string.app_name),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) {
          Icon(
            painterResource(R.drawable.ic_ui_search),
            contentDescription = stringResource(R.string.home_search),
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
    if (state.loading) {
      item(key = "loading") {
        Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
          PulsingPlaceholder(
            Modifier.fillMaxWidth().height(spotlightHeight).padding(horizontal = 20.dp),
            cornerRadius = 24.dp
          )
          Row(
            Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            repeat(3) { PulsingPlaceholder(Modifier.size(144.dp, 208.dp)) }
          }
        }
      }
    } else {
      state.topPick?.let { movie ->
        item(key = "spotlight") {
          SpotlightCard(
            movie = movie,
            onClick = { onMovieClick(movie.id) },
            modifier = Modifier.padding(horizontal = 20.dp),
            height = spotlightHeight,
            actions = {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                MovieActionButton(
                  MovieAction.LIKE, movie.id in state.likedIds, { onLike(movie) }
                )
                MovieActionButton(
                  MovieAction.WATCH_LATER, movie.id in state.watchLaterIds, { onWatchLater(movie) }
                )
              }
            }
          )
        }
      }
      if (state.loadFailed) {
        item(key = "error") { HomeMessage(R.string.home_load_error, true, onRetry) }
      } else if (state.rails.isEmpty()) {
        item(key = "empty") { HomeMessage(R.string.home_empty, true, onRetry) }
      }
      items(state.rails, key = { it.sourceMovie?.id ?: "trending" }) { rail ->
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Text(
            text = rail.sourceMovie?.let {
              stringResource(R.string.home_because_you_liked, it.title)
            } ?: stringResource(R.string.home_trending),
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
          )
          when {
            rail.failed -> HomeMessage(R.string.home_load_error, true, onRetry)
            rail.movies.isEmpty() -> HomeMessage(R.string.home_empty, false, onRetry)
            else -> LazyRow(
              contentPadding = PaddingValues(horizontal = 20.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              items(rail.movies, key = Movie::id) { movie ->
                MovieCard(movie, onClick = { onMovieClick(movie.id) })
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HomeMessage(message: Int, retry: Boolean, onRetry: () -> Unit) {
  Column(
    Modifier.padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      stringResource(message),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onBackground
    )
    if (retry) TextButton(onClick = onRetry) { Text(stringResource(R.string.home_retry)) }
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun HomePreview() {
  val movie = Movie(1, "The Grand Budapest Hotel", null, 2014)
  MovaraTheme {
    HomeContent(HomeUiState(loading = false, rails = listOf(HomeRail(movies = listOf(movie)))),
      {}, {}, {}, {}, {})
  }
}
