package ee.movara.app.ui.preferences

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ee.movara.app.R
import ee.movara.app.domain.model.GenreCatalog
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieTastePreferences
import ee.movara.app.ui.components.MovieSearchField
import ee.movara.app.ui.components.TmdbImage
import ee.movara.app.ui.theme.MovaraTheme
import kotlinx.coroutines.flow.collect

@Composable
fun MoviePreferenceQuizScreen(
  viewModel: MoviePreferenceQuizViewModel,
  isEditing: Boolean,
  onFinished: () -> Unit,
  onCancel: () -> Unit
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  LaunchedEffect(state.finished) {
    if (state.finished) onFinished()
  }
  PredictiveBackHandler(
    enabled = state.saving || state.step != QuizStep.GENRES || isEditing
  ) { progress ->
    progress.collect()
    val current = viewModel.uiState.value
    if (!current.saving) {
      if (current.step == QuizStep.GENRES) onCancel() else viewModel.previous()
    }
  }
  MoviePreferenceQuizContent(
    state = state,
    isEditing = isEditing,
    actions = QuizActions(
      toggleGenre = viewModel::toggleGenre,
      clearGenres = viewModel::clearGenres,
      toggleMovie = viewModel::toggleMovie,
      removeFavourite = viewModel::removeFavourite,
      clearFavourites = viewModel::clearFavourites,
      queryChanged = viewModel::onQueryChange,
      search = viewModel::submitSearch,
      more = viewModel::loadMore,
      retryMovies = viewModel::retryMovies,
      retryProfile = viewModel::retryProfile,
      ratingSelected = viewModel::selectRating,
      back = viewModel::previous,
      next = { if (state.step == QuizStep.RATING) viewModel.finish() else viewModel.next() },
      exit = { if (isEditing) onCancel() else viewModel.skip() },
      continueWithoutSaving = onFinished
    )
  )
}

internal data class QuizActions(
  val toggleGenre: (Int) -> Unit = {},
  val clearGenres: () -> Unit = {},
  val toggleMovie: (Movie) -> Unit = {},
  val removeFavourite: (Int) -> Unit = {},
  val clearFavourites: () -> Unit = {},
  val queryChanged: (String) -> Unit = {},
  val search: () -> Unit = {},
  val more: () -> Unit = {},
  val retryMovies: () -> Unit = {},
  val retryProfile: () -> Unit = {},
  val ratingSelected: (Int?) -> Unit = {},
  val back: () -> Unit = {},
  val next: () -> Unit = {},
  val exit: () -> Unit = {},
  val continueWithoutSaving: () -> Unit = {}
)

@Composable
internal fun MoviePreferenceQuizContent(
  state: MoviePreferenceQuizState,
  isEditing: Boolean = false,
  actions: QuizActions = QuizActions()
) {
  val editable = !state.loadingProfile && !state.profileLoadFailed && !state.saving
  Surface(color = MaterialTheme.colorScheme.background) {
    Column(
      Modifier.fillMaxSize()
        .safeDrawingPadding().imePadding(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Column(
        Modifier.widthIn(max = 840.dp).fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          Modifier.fillMaxWidth().heightIn(min = 56.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
          )
          TextButton(onClick = actions.exit, enabled = !state.saving) {
            Text(stringResource(if (isEditing) R.string.quiz_cancel else R.string.quiz_skip))
          }
        }
        Text(
          stringResource(R.string.quiz_step, state.step.ordinal + 1),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
          progress = { (state.step.ordinal + 1) / 3f },
          modifier = Modifier.fillMaxWidth()
        )
        when {
          state.loadingProfile -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
            LoadingMessage(R.string.quiz_profile_loading)
          }
          state.profileLoadFailed -> Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center
          ) {
            ErrorMessage(R.string.quiz_profile_error, actions.retryProfile)
          }
          else -> {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                stringResource(when (state.step) {
                  QuizStep.GENRES -> R.string.quiz_genres_title
                  QuizStep.MOVIES -> R.string.quiz_movies_title
                  QuizStep.RATING -> R.string.quiz_rating_title
                }),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
              )
              Text(
                stringResource(when (state.step) {
                  QuizStep.GENRES -> R.string.quiz_genres_description
                  QuizStep.MOVIES -> R.string.quiz_movies_description
                  QuizStep.RATING -> R.string.quiz_rating_description
                }),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              when (state.step) {
                QuizStep.GENRES -> GenreChoices(state, editable, actions)
                QuizStep.MOVIES -> FavouriteChoices(state, editable, actions)
                QuizStep.RATING -> RatingChoices(state, editable, actions)
              }
            }
          }
        }
        if (state.saveFailed) {
          Text(
            stringResource(R.string.quiz_save_error),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
          )
          if (!isEditing) {
            TextButton(onClick = actions.continueWithoutSaving) {
              Text(stringResource(R.string.quiz_continue_without_saving))
            }
          }
        }
        Row(
          Modifier.fillMaxWidth().padding(bottom = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (state.step != QuizStep.GENRES) {
            OutlinedButton(onClick = actions.back, enabled = editable) {
              Text(stringResource(R.string.quiz_back))
            }
          }
          Button(
            onClick = actions.next,
            enabled = editable,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
          ) {
            Text(stringResource(when {
              state.saving -> R.string.quiz_saving
              state.step != QuizStep.RATING -> R.string.quiz_next
              isEditing -> R.string.quiz_save
              else -> R.string.quiz_finish
            }))
          }
        }
      }
    }
  }
}

@Composable
private fun GenreChoices(state: MoviePreferenceQuizState, enabled: Boolean, actions: QuizActions) {
  LazyColumn(
    Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        GenreCatalog.genres.forEach { genre ->
          FilterChip(
            selected = genre.id in state.genreIds,
            onClick = { actions.toggleGenre(genre.id) },
            enabled = enabled && (
              genre.id in state.genreIds || state.genreIds.size < MovieTastePreferences.MAX_GENRES
            ),
            label = { Text(genre.name) },
            modifier = Modifier.heightIn(min = 48.dp)
          )
        }
      }
    }
    item {
      FilterChip(
        selected = state.genreIds.isEmpty(),
        onClick = actions.clearGenres,
        enabled = enabled,
        label = { Text(stringResource(R.string.quiz_no_preference)) },
        modifier = Modifier.heightIn(min = 48.dp)
      )
    }
    item {
      Text(
        stringResource(R.string.quiz_genre_count, state.genreIds.size),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
      )
    }
  }
}

@Composable
private fun FavouriteChoices(
  state: MoviePreferenceQuizState,
  enabled: Boolean,
  actions: QuizActions
) {
  val keyboard = LocalSoftwareKeyboardController.current
  LazyVerticalGrid(
    columns = GridCells.Adaptive(100.dp),
    modifier = Modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 12.dp)
  ) {
    item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
      MovieSearchField(
        query = state.query,
        onQueryChange = actions.queryChanged,
        enabled = enabled,
        onSearch = { actions.search(); keyboard?.hide() }
      )
    }
    item(key = "selection", span = { GridItemSpan(maxLineSpan) }) {
      Column {
        Row(
          Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            stringResource(R.string.quiz_selected_count, state.favouriteMovieIds.size),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
          )
          if (state.favouriteMovieIds.isNotEmpty()) {
            TextButton(onClick = actions.clearFavourites, enabled = enabled) {
              Text(stringResource(R.string.quiz_clear_selection))
            }
          }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(state.favouriteTitles.toList(), key = { it.first }) { (id, title) ->
            val removeLabel = stringResource(R.string.quiz_remove_favourite, title)
            InputChip(
              selected = true,
              onClick = { actions.removeFavourite(id) },
              enabled = enabled,
              label = {
                Text(
                  title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.widthIn(max = 180.dp)
                )
              },
              trailingIcon = {
                Icon(painterResource(R.drawable.ic_ui_close), null, Modifier.size(16.dp))
              },
              modifier = Modifier.heightIn(min = 48.dp).semantics {
                contentDescription = removeLabel
              }
            )
          }
        }
        Text(
          stringResource(R.string.quiz_selection_hint),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
    items(state.movies, key = Movie::id) { movie ->
      FavouriteMovieCard(
        movie, movie.id in state.favouriteMovieIds, enabled,
        onClick = { actions.toggleMovie(movie) }
      )
    }
    item(key = "status", span = { GridItemSpan(maxLineSpan) }) {
      when {
        state.loadingMovies -> LoadingMessage(R.string.quiz_movies_loading)
        state.movieLoadFailed -> ErrorMessage(R.string.quiz_movies_error, actions.retryMovies)
        state.movies.isEmpty() -> Text(stringResource(R.string.quiz_movies_empty))
        state.canLoadMore -> OutlinedButton(onClick = actions.more, enabled = enabled) {
          Text(stringResource(R.string.quiz_show_more))
        }
      }
    }
  }
}

@Composable
private fun FavouriteMovieCard(
  movie: Movie,
  selected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(12.dp)
  Column(
    Modifier.fillMaxWidth().clip(shape)
      .background(MaterialTheme.colorScheme.surface)
      .border(
        BorderStroke(
          if (selected) 2.dp else 1.dp,
          if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ), shape
      )
      .toggleable(selected, enabled = enabled, role = Role.Checkbox) { onClick() }
      .padding(6.dp)
  ) {
    Box {
      TmdbImage(movie.posterPath, Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(shape))
      Checkbox(
        checked = selected,
        onCheckedChange = null,
        modifier = Modifier.align(Alignment.TopEnd)
          .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), shape)
      )
    }
    Text(
      movie.title,
      style = MaterialTheme.typography.labelLarge,
      maxLines = 2, overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(top = 8.dp)
    )
    movie.releaseYear?.let {
      Text(
        it.toString(), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun RatingChoices(state: MoviePreferenceQuizState, enabled: Boolean, actions: QuizActions) {
  LazyColumn(
    Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    item {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(null, 6, 7, 8).forEach { rating ->
          FilterChip(
            selected = state.minimumRating == rating,
            onClick = { actions.ratingSelected(rating) },
            enabled = enabled,
            label = {
              Text(if (rating == null) stringResource(R.string.quiz_rating_any)
                else stringResource(R.string.quiz_rating_value, rating))
            },
            modifier = Modifier.heightIn(min = 48.dp)
          )
        }
      }
    }
    item {
      Text(
        stringResource(R.string.quiz_rating_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    item {
      Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(stringResource(R.string.quiz_summary), style = MaterialTheme.typography.titleMedium)
          Text(stringResource(
            R.string.quiz_summary_counts, state.genreIds.size, state.favouriteMovieIds.size
          ))
          Text(
            stringResource(R.string.quiz_local_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun LoadingMessage(message: Int) {
  Row(
    Modifier.fillMaxWidth().padding(vertical = 20.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
    Text(stringResource(message), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
  }
}

@Composable
private fun ErrorMessage(message: Int, onRetry: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      stringResource(message), color = MaterialTheme.colorScheme.error,
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    )
    TextButton(onClick = onRetry) { Text(stringResource(R.string.quiz_retry)) }
  }
}

@Composable
fun PreferenceStartupScreen(loading: Boolean, onRetry: () -> Unit, onContinue: () -> Unit) {
  Surface(color = MaterialTheme.colorScheme.background) {
    Column(
      Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(Modifier.height(24.dp))
      if (loading) LoadingMessage(R.string.quiz_profile_loading) else {
        ErrorMessage(R.string.quiz_profile_error, onRetry)
        TextButton(onClick = onContinue) {
          Text(stringResource(R.string.quiz_continue_for_now))
        }
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Preview(name = "Medium", showBackground = true, widthDp = 700, heightDp = 840)
@Composable
private fun GenreQuizPreview() {
  MovaraTheme {
    MoviePreferenceQuizContent(
      MoviePreferenceQuizState(loadingProfile = false, genreIds = setOf(28, 878))
    )
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun MovieQuizPreview() {
  MovaraTheme {
    MoviePreferenceQuizContent(MoviePreferenceQuizState(
      loadingProfile = false, step = QuizStep.MOVIES,
      movies = listOf(Movie(603, "The Matrix", null, 1999), Movie(550, "Fight Club", null, 1999)),
      favouriteMovieIds = setOf(603), favouriteTitles = mapOf(603 to "The Matrix")
    ))
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun RatingQuizPreview() {
  MovaraTheme {
    MoviePreferenceQuizContent(MoviePreferenceQuizState(
      loadingProfile = false, step = QuizStep.RATING,
      genreIds = setOf(28, 878), favouriteMovieIds = setOf(603, 550, 680), minimumRating = 7
    ))
  }
}
