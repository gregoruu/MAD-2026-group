package ee.movara.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ee.movara.app.R
import ee.movara.app.domain.model.GenreCatalog
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.recommendation.MovieRecommendation
import ee.movara.app.domain.recommendation.RecommendationSource
import ee.movara.app.ui.components.MovieAction
import ee.movara.app.ui.components.MovieActionButton
import ee.movara.app.ui.components.MovieCard
import ee.movara.app.ui.components.PulsingPlaceholder
import ee.movara.app.ui.components.SpotlightCard
import ee.movara.app.ui.layout.LocalScreenLayout
import ee.movara.app.ui.layout.ScreenLayoutForAvailableSize
import ee.movara.app.ui.theme.MovaraTheme
import ee.movara.app.ui.components.EmptyState
import ee.movara.app.ui.state.UiState

@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  onSearch: () -> Unit,
  onMovieClick: (Int) -> Unit,
  onEditPreferences: () -> Unit,
  modifier: Modifier = Modifier
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  Box(modifier.fillMaxSize()) {
    HomeContent(
      state = state,
      onSearch = onSearch,
      onMovieClick = onMovieClick,
      onLike = viewModel::toggleLike,
      onWatchLater = viewModel::toggleWatchLater,
      onRetry = viewModel::retry,
      onEditPreferences = onEditPreferences
    )
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
  modifier: Modifier = Modifier,
  onEditPreferences: () -> Unit = {}
) {
  val configuration = LocalConfiguration.current
  val layout = LocalScreenLayout.current
  Box(modifier.fillMaxSize()) {
    // Medium screens show the Spotlight as a wide card with the poster on the left
    val horizontalSpotlight = layout.isMedium
    val screenMargin = layout.screenMargin
    val spotlightHeight = configuration.screenHeightDp.dp * 0.62f
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
      item(key = "header") {
        Row(
          modifier = Modifier.fillMaxWidth().padding(
            start = screenMargin,
            end = if (horizontalSpotlight) screenMargin else 12.dp,
            top = 8.dp
          ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
          Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onEditPreferences) {
              Text(stringResource(R.string.preferences_edit))
            }
            IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) {
              Icon(
                painterResource(R.drawable.ic_ui_search),
                contentDescription = stringResource(R.string.home_search),
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
      if (state.contentState == UiState.Loading) {
        item(key = "loading") {
          Column(
            modifier = Modifier.testTag("home_loading"),
            verticalArrangement = Arrangement.spacedBy(32.dp)
          ) {
            PulsingPlaceholder(
              Modifier.fillMaxWidth().height(spotlightHeight).padding(horizontal = screenMargin),
              cornerRadius = 24.dp
            )
            Row(
              Modifier.padding(horizontal = screenMargin),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              repeat(3) { PulsingPlaceholder(Modifier.size(144.dp, 208.dp)) }
            }
          }
        }
      } else {
        if (state.usingCache) {
          item(key = "cache") {
            Text(
              stringResource(R.string.home_cached),
              Modifier.padding(horizontal = screenMargin),
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
        if (state.loadFailed) {
          item(key = "error") {
            HomeMessage(
              stringResource(if (state.recommendations.isEmpty()) R.string.home_load_error
                else R.string.home_partial_error),
              true, onRetry, screenMargin
            )
          }
        }
        state.topRecommendation?.let { recommendation ->
          val movie = recommendation.movie
          item(key = "spotlight") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                recommendationReason(recommendation),
                Modifier.padding(horizontal = screenMargin),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              SpotlightCard(
                movie = movie,
                onClick = { onMovieClick(movie.id) },
                modifier = Modifier.padding(horizontal = screenMargin),
                height = spotlightHeight,
                horizontal = horizontalSpotlight,
                actions = {
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    MovieActionButton(
                      MovieAction.LIKE, movie.id in state.likedIds, { onLike(movie) },
                      enabled = state.actionsEnabled && movie.id !in state.pendingIds
                    )
                    MovieActionButton(
                      MovieAction.WATCH_LATER, movie.id in state.watchLaterIds, { onWatchLater(movie) },
                      enabled = state.actionsEnabled && movie.id !in state.pendingIds
                    )
                  }
                }
              )
            }
          }
        }
        if (state.contentState == UiState.Empty) {
          item(key = "empty") {
            val message = state.minimumRating?.let {
              stringResource(R.string.home_empty_rating, it)
            } ?: stringResource(R.string.home_empty)
            Column(
              Modifier.padding(horizontal = screenMargin),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              EmptyState(message = message)
              TextButton(onClick = onRetry) { Text(stringResource(R.string.home_retry)) }
            }
          }
        }
        if (state.recommendations.size > 1) {
          item(key = "recommendations") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Text(
                stringResource(if (state.recommendations.any { !it.isDiscovery })
                  R.string.home_for_you else R.string.home_more_discovery),
                Modifier.padding(horizontal = screenMargin),
                style = MaterialTheme.typography.headlineSmall
              )
              LazyRow(
                contentPadding = PaddingValues(horizontal = screenMargin),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                items(state.recommendations.drop(1), key = { it.movie.id }) { recommendation ->
                  Column(
                    Modifier.width(144.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    MovieCard(recommendation.movie, onClick = { onMovieClick(recommendation.movie.id) })
                    Text(
                      recommendationReason(recommendation),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun recommendationReason(item: MovieRecommendation): String {
  val seeds = item.sources.filterIsInstance<RecommendationSource.Seed>().sortedBy { it.movieId }
  val title = seeds.firstNotNullOfOrNull { it.title?.takeIf(String::isNotBlank) }
  return when {
    title != null -> stringResource(R.string.home_because_you_enjoyed, title)
    seeds.isNotEmpty() -> stringResource(R.string.home_from_favourites)
    item.preferredGenreIds.isNotEmpty() -> stringResource(
      R.string.home_preferred_genre, GenreCatalog.nameForId(item.preferredGenreIds.min())
    )
    else -> stringResource(R.string.home_trending)
  }
}

@Composable
private fun HomeMessage(message: String, retry: Boolean, onRetry: () -> Unit, screenMargin: Dp) {
  Column(
    Modifier.padding(horizontal = screenMargin),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(message, style = MaterialTheme.typography.bodyLarge)
    if (retry) TextButton(onClick = onRetry) { Text(stringResource(R.string.home_retry)) }
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Preview(name = "Medium", showBackground = true, widthDp = 700, heightDp = 840)
@Composable
private fun HomePreview() {
  val movie = Movie(1, "The Grand Budapest Hotel", null, 2014)
  MovaraTheme {
    ScreenLayoutForAvailableSize {
      HomeContent(
        HomeUiState(loading = false, recommendations = listOf(MovieRecommendation(movie))),
        {}, {}, {}, {}, {}
      )
    }
  }
}
