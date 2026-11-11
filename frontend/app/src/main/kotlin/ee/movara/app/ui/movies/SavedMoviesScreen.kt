package ee.movara.app.ui.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import ee.movara.app.R
import ee.movara.app.data.local.MovieListType
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.components.MovieRow
import ee.movara.app.ui.layout.LocalScreenLayout
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
  val layout = LocalScreenLayout.current
  Box(Modifier.fillMaxSize()) {
    val columns = layout.savedMovieColumns
    val margin = layout.screenMargin
    Column(
      Modifier.fillMaxSize().padding(horizontal = margin),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("My Movies", Modifier.weight(1f), style = MaterialTheme.typography.displayMedium)
        IconButton(onClick = onAbout) {
          Icon(painterResource(R.drawable.ic_ui_info), contentDescription = "About")
        }
      }
      SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        MovieListType.entries.forEachIndexed { index, type ->
          SegmentedButton(
            selected = selected == type,
            onClick = { selected = type },
            shape = SegmentedButtonDefaults.itemShape(index, MovieListType.entries.size, RoundedCornerShape(8.dp)),
            modifier = Modifier.heightIn(min = 48.dp),
            colors = SegmentedButtonDefaults.colors(
              activeContainerColor = MovaraGold,
              activeContentColor = MovaraBackground,
              inactiveContainerColor = MovaraSurface,
              inactiveContentColor = MovaraTextSecondary
            )
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(type.label, style = MaterialTheme.typography.labelSmall)
              Text("${state.lists.movies(type).size}", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
      if (state.isLoading) CircularProgressIndicator()
      state.storageError?.let {
        Text(it)
        TextButton(onClick = onRetry) { Text("Retry") }
      }
      val movies = state.lists.movies(selected)
      if (!state.isLoading && state.storageError == null && movies.isEmpty()) {
        Text("No movies yet.", color = MovaraTextSecondary)
      }
      // Each tab retains its own scroll position, including after opening movie details.
      tabStates.SaveableStateProvider(selected.name) {
        if (columns > 1) {
          LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(bottom = margin),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            items(movies, key = { it.id }) { movie ->
              SavedMovieItem(movie, state, onOpenMovie, onMembershipChange)
            }
          }
        } else {
          LazyColumn(
            contentPadding = PaddingValues(bottom = margin),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            items(movies, key = { it.id }) { movie ->
              SavedMovieItem(movie, state, onOpenMovie, onMembershipChange)
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
  state: MovieListsUiState,
  onOpenMovie: (Int) -> Unit,
  onMembershipChange: (Movie, MovieListType, Boolean) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    MovieRow(movie = movie, onClick = { onOpenMovie(movie.id) })
    MovieMembershipActions(movie, state, onMembershipChange)
  }
}
