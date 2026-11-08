package ee.movara.app.ui.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import ee.movara.app.data.local.MovieListType
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.components.MovieRow

@Composable
fun SavedMoviesScreen(
  state: MovieListsUiState,
  onMembershipChange: (Movie, MovieListType, Boolean) -> Unit,
  onOpenMovie: (Int) -> Unit,
  onAbout: () -> Unit,
  onRetry: () -> Unit
) {
  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    item {
      Text("My Movies", style = MaterialTheme.typography.headlineMedium)
      TextButton(onClick = onAbout) { Text("About") }
      if (state.isLoading) CircularProgressIndicator()
      state.storageError?.let {
        Text(it)
        TextButton(onClick = onRetry) { Text("Retry") }
      }
    }
    MovieListType.entries.forEach { type ->
      item(key = type.name) {
        Text(when (type) {
          MovieListType.LIKED -> "Liked"
          MovieListType.WATCH_LATER -> "Watch Later"
          MovieListType.WATCHED -> "Watched"
        }, style = MaterialTheme.typography.titleLarge)
        Text("${state.lists.movies(type).size} movies")
        if (!state.isLoading && state.storageError == null && state.lists.movies(type).isEmpty()) Text("No movies yet.")
      }
      items(state.lists.movies(type), key = { "${type.name}:${it.id}" }) { movie ->
        MovieRow(movie = movie, onClick = { onOpenMovie(movie.id) })
        MovieMembershipActions(movie, state, onMembershipChange)
      }
    }
  }
}
