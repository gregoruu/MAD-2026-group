package ee.movara.app.ui.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ee.movara.app.R
import ee.movara.app.data.local.MovieListType
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.components.MovieRow
import ee.movara.app.ui.components.MovieAction
import ee.movara.app.ui.components.MovieActionButton
import ee.movara.app.ui.components.EmptyState
import ee.movara.app.ui.components.PulsingPlaceholder
import ee.movara.app.ui.theme.*

internal val MovieListType.label: String
  get() = when (this) {
    MovieListType.LIKED -> "Liked"
    MovieListType.WATCH_LATER -> "Watch Later"
    MovieListType.WATCHED -> "Watched"
  }

@Composable
fun SavedMoviesScreen(
  state: MovieListsUiState,
  onMembershipChange: (Movie, MovieListType, Boolean) -> Unit,
  onOpenMovie: (Int) -> Unit,
  onAbout: () -> Unit,
  onRetry: () -> Unit
) {
  var selected by rememberSaveable { mutableStateOf(MovieListType.LIKED) }
  val tabStates = rememberSaveableStateHolder()
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val medium = maxWidth >= 600.dp
    val margin = if (medium) MovaraSpacing.screenMarginExpanded else MovaraSpacing.screenMargin
    Column(
      Modifier.fillMaxSize().padding(horizontal = margin).padding(top = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.my_movies_title), Modifier.weight(1f), style = MaterialTheme.typography.displayMedium)
        IconButton(onClick = onAbout) {
          Icon(painterResource(R.drawable.ic_ui_info), contentDescription = stringResource(R.string.about_title))
        }
      }
      SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        MovieListType.entries.forEachIndexed { index, type ->
          SegmentedButton(
            selected = selected == type,
            onClick = { selected = type },
            icon = {},
            shape = SegmentedButtonDefaults.itemShape(index, MovieListType.entries.size, RoundedCornerShape(8.dp)),
            modifier = Modifier.heightIn(min = 48.dp),
            colors = SegmentedButtonDefaults.colors(
              activeContainerColor = MovaraGold,
              activeContentColor = MovaraBackground,
              inactiveContainerColor = MovaraSurface,
              inactiveContentColor = MovaraTextSecondary,
              activeBorderColor = MovaraGold,
              inactiveBorderColor = MovaraBorder
            )
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(type.label, style = MaterialTheme.typography.labelSmall)
              Text(if (state.isLoading) "…" else "${state.lists.movies(type).size}", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
      val movies = state.lists.movies(selected)
      // Each tab retains its own scroll position, including after opening movie details.
      tabStates.SaveableStateProvider(selected.name) {
        when {
          state.isLoading -> {
            val loadingLabel = stringResource(R.string.my_movies_loading)
            LazyVerticalGrid(
              columns = GridCells.Fixed(if (medium) 2 else 1),
              modifier = Modifier.weight(1f).semantics { contentDescription = loadingLabel },
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
              contentPadding = PaddingValues(bottom = margin)
            ) {
              items(6) { PulsingPlaceholder(Modifier.fillMaxWidth().height(96.dp)) }
            }
          }
          state.storageError != null -> {
            Column(
              Modifier.weight(1f).fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              EmptyState(stringResource(R.string.my_movies_storage_error))
              TextButton(onClick = onRetry) { Text(stringResource(R.string.home_retry)) }
            }
          }
          movies.isEmpty() -> {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
              EmptyState(
                message = stringResource(when (selected) {
                  MovieListType.LIKED -> R.string.my_movies_empty_liked
                  MovieListType.WATCH_LATER -> R.string.my_movies_empty_watch_later
                  MovieListType.WATCHED -> R.string.my_movies_empty_watched
                }),
                iconRes = when (selected) {
                  MovieListType.LIKED -> R.drawable.ic_ui_favorite_outline
                  MovieListType.WATCH_LATER -> R.drawable.ic_ui_bookmark_outline
                  MovieListType.WATCHED -> R.drawable.ic_ui_visibility
                }
              )
            }
          }
          else -> {
            LazyVerticalGrid(
              columns = GridCells.Fixed(if (medium) 2 else 1),
              modifier = Modifier.weight(1f),
              contentPadding = PaddingValues(bottom = margin),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              items(movies, key = { it.id }) { movie ->
                SavedMovieItem(movie, selected, state, onOpenMovie, onMembershipChange)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SavedMovieItem(
  movie: Movie,
  selected: MovieListType,
  state: MovieListsUiState,
  onOpenMovie: (Int) -> Unit,
  onMembershipChange: (Movie, MovieListType, Boolean) -> Unit
) {
  MovieRow(
    movie = movie,
    onClick = { onOpenMovie(movie.id) },
    actions = if (selected == MovieListType.WATCH_LATER) {
      {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          MovieActionButton(
            action = MovieAction.WATCHED,
            isSelected = false,
            onClick = { onMembershipChange(movie, MovieListType.WATCHED, true) },
            enabled = state.actionsEnabled && movie.id !in state.pendingIds
          )
          IconButton(
            onClick = { onMembershipChange(movie, MovieListType.WATCH_LATER, false) },
            enabled = state.actionsEnabled && movie.id !in state.pendingIds
          ) {
            Icon(
              painterResource(R.drawable.ic_ui_close),
              stringResource(R.string.my_movies_remove, movie.title),
              Modifier.size(20.dp)
            )
          }
        }
      }
    } else null
  )
}
