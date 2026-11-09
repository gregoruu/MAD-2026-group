package ee.movara.app.ui.movies

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import ee.movara.app.data.local.MovieListType
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.components.MovieAction
import ee.movara.app.ui.components.MovieActionButton

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
