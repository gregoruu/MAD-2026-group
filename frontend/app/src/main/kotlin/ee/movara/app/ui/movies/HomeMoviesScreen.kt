package ee.movara.app.ui.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ee.movara.app.data.local.MovieListType
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.components.MovieAction
import ee.movara.app.ui.components.MovieActionButton
import ee.movara.app.ui.components.MovieRow
import ee.movara.app.ui.home.HomeUiState

@Composable
fun HomeMoviesScreen(
  state: HomeUiState,
  lists: MovieListsUiState,
  onMembershipChange: (Movie, MovieListType, Boolean) -> Unit,
  onOpenMovie: (Int) -> Unit,
  onSearch: () -> Unit,
  onRetry: () -> Unit
) {
  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    item {
      Text("Trending this week", style = MaterialTheme.typography.headlineMedium)
      TextButton(onClick = onSearch) { Text("Search") }
      if (state.isLoading || lists.isLoading) CircularProgressIndicator()
      state.error?.let { Text(it) }
      lists.storageError?.let { Text(it) }
      if (state.error != null || lists.storageError != null) TextButton(onClick = onRetry) { Text("Retry") }
      if (!state.isLoading && state.error == null && state.movies.isEmpty()) Text("No unwatched movies available.")
    }
    items(state.movies, key = { it.id }) { movie ->
      MovieRow(movie = movie, onClick = { onOpenMovie(movie.id) })
      MovieMembershipActions(movie, lists, onMembershipChange)
    }
  }
}

@Composable
internal fun MovieMembershipActions(
  movie: Movie,
  state: MovieListsUiState,
  onMembershipChange: (Movie, MovieListType, Boolean) -> Unit
) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    MovieListType.entries.forEach { type ->
      val selected = state.lists.contains(movie.id, type)
      MovieActionButton(
        action = when (type) {
          MovieListType.LIKED -> MovieAction.LIKE
          MovieListType.WATCH_LATER -> MovieAction.WATCH_LATER
          MovieListType.WATCHED -> MovieAction.WATCHED
        },
        isSelected = selected,
        enabled = state.actionsEnabled && movie.id !in state.pendingIds,
        onClick = { onMembershipChange(movie, type, !selected) }
      )
    }
  }
}
