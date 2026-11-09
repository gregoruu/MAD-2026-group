package ee.movara.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ee.movara.app.R
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.components.MovieSearchField
import ee.movara.app.ui.components.PulsingPlaceholder
import ee.movara.app.ui.components.TmdbImage
import ee.movara.app.ui.theme.MovaraBackground
import ee.movara.app.ui.theme.MovaraGold
import ee.movara.app.ui.theme.MovaraText
import ee.movara.app.ui.theme.MovaraTextSecondary
import androidx.compose.ui.platform.testTag
import ee.movara.app.ui.components.EmptyState
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

@Composable
fun SearchScreen(
  state: SearchUiState,
  onQueryChange: (String) -> Unit,
  onSearch: () -> Unit,
  onLoadMore: () -> Unit,
  onRetry: () -> Unit,
  onMovieClick: (Int) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val keyboard = LocalSoftwareKeyboardController.current
  val gridState = rememberLazyGridState()
  val isMediumOrWider = LocalConfiguration.current.screenWidthDp >= 600
  val columnCount = if (isMediumOrWider) 5 else 3
  val screenMargin = if (isMediumOrWider) 24.dp else 20.dp
  val loadingDescription = stringResource(R.string.search_loading)
  LaunchedEffect(state.query) {
    gridState.scrollToItem(0)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MovaraBackground)
      .padding(horizontal = screenMargin),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(top = 12.dp)
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(
          painter = painterResource(R.drawable.ic_ui_arrow_back),
          contentDescription = stringResource(R.string.search_back),
          tint = MovaraGold
        )
      }
      Text(
        stringResource(R.string.search_title),
        style = MaterialTheme.typography.displayMedium,
        color = MovaraText
      )
    }

    MovieSearchField(
      query = state.query,
      onQueryChange = onQueryChange,
      onSearch = {
        onSearch()
        if (state.query.isNotBlank()) keyboard?.hide()
      }
    )

    if (state.showTitleError) {
      Text(
        stringResource(R.string.search_enter_title),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
      )
    }

    LazyVerticalGrid(
      columns = GridCells.Fixed(columnCount),
      state = gridState,
      modifier = Modifier.weight(1f),
      contentPadding = PaddingValues(bottom = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      items(state.movies, key = Movie::id) { movie ->
        SearchMovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
      }
      if (state.isLoading) {
        items(columnCount * 2) { index ->
          PulsingPlaceholder(
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(2f / 3f)
              .semantics {
                if (index == 0) {
                  contentDescription = loadingDescription
                  liveRegion = LiveRegionMode.Polite
                }
              }
          )
        }
      }
      item(span = { GridItemSpan(maxLineSpan) }) {
        when {
          state.isLoading -> Unit
          state.requestError != null -> Column {
            Text(
              state.requestError,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            TextButton(onClick = onRetry) {
              Text(stringResource(R.string.search_retry))
            }
          }

          state.page > 0 && state.movies.isEmpty() -> EmptyState(
            message = stringResource(R.string.search_no_results),
            iconRes = R.drawable.ic_ui_sentiment_dissatisfied,
            modifier = Modifier
              .testTag("search_empty_state")
              .semantics {
                liveRegion = LiveRegionMode.Polite
              }
          )


          state.canLoadMore -> TextButton(onClick = onLoadMore) {
            Text(stringResource(R.string.search_load_more))
          }
        }
      }
    }
  }
}

// A grid card grows with its column, keeping the poster at the specified 2:3 ratio.
@Composable
private fun SearchMovieCard(movie: Movie, onClick: () -> Unit) {
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MovaraBackground)
  ) {
    TmdbImage(
      movie.posterPath,
      modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
      size = "w342"
    )
    Column(
      modifier = Modifier.padding(top = 8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        movie.title,
        style = MaterialTheme.typography.titleSmall,
        color = MovaraText,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      movie.releaseYear?.let {
        Text(
          it.toString(),
          style = MaterialTheme.typography.bodySmall,
          color = MovaraTextSecondary
        )
      }
    }
  }
}
