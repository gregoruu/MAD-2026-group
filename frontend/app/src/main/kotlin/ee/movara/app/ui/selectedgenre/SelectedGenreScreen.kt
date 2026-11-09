
package ee.movara.app.ui.selectedgenre

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.movara.app.R
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.components.MovieCard
import ee.movara.app.ui.components.PulsingPlaceholder
import ee.movara.app.ui.theme.*
import kotlinx.coroutines.flow.first
import ee.movara.app.domain.model.GenreCatalog
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import ee.movara.app.ui.state.UiState
import ee.movara.app.ui.state.listUiState


// displays movies belonging to the selected genre
@Composable
fun SelectedGenreScreen(
    genreId: Int,
    movies: List<Movie>,
    onBack: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    loadMoreError: String? = null
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MovaraBackground)
    ) {
        // Three columns on phones, five on medium screens
        val isMediumScreen = maxWidth >= 600.dp
        val columns = if (isMediumScreen) 5 else 3
        val screenPadding = if (isMediumScreen) 24.dp else 20.dp
        val gridSpacing = if (isMediumScreen) 12.dp else 8.dp

        Column(modifier = Modifier.fillMaxSize()) {

            // Back navigation
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(
                    start = screenPadding,
                    top = 8.dp
                )
            ) {
                Icon(
                    painter = painterResource(
                        R.drawable.ic_ui_arrow_back
                    ),
                    contentDescription = null,
                    tint = MovaraText,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.selected_genre_back),
                    color = MovaraText
                )
            }

            Text(
                text = GenreCatalog.nameForId(genreId),
                style = MaterialTheme.typography.displayMedium,
                color = MovaraGold,
                modifier = Modifier.padding(
                    start = screenPadding,
                    end = screenPadding,
                    top = 8.dp,
                    bottom = 16.dp
                )
            )

            val contentState = listUiState(
                items = movies,
                loading = isLoading,
                errorMessage = errorMessage
            )

            when {
                // Show placeholder movie cards initially
                contentState == UiState.Loading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = screenPadding,
                            vertical = 8.dp
                        ),
                        horizontalArrangement =
                            Arrangement.spacedBy(gridSpacing),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        items(columns * 3) {
                            PulsingPlaceholder(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(144f / 208f)
                                    .testTag("genre_loading_placeholder"),
                                cornerRadius = 12.dp
                            )
                        }
                    }
                }

                // Initial loading error
                contentState is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.Center
                    ) {
                        Text(
                            text = contentState.message,
                            color = MovaraText,
                            style =
                                MaterialTheme.typography.bodyLarge
                        )

                        // Only show retry when it is functional.
                        if (onRetry != null) {
                            Spacer(Modifier.height(16.dp))

                            Button(onClick = onRetry) {
                                Text(
                                    text = stringResource(R.string.selected_genre_retry)
                                )
                            }
                        }
                    }
                }

                // No movies were returned
                contentState == UiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.selected_genre_empty),
                            color = MovaraTextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                else -> {
                    val gridState = rememberLazyGridState()

                    val latestOnLoadMore by rememberUpdatedState(
                        onLoadMore
                    )

                    // Request another page near the bottom
                    LaunchedEffect(
                        genreId,
                        gridState,
                        movies.size,
                        hasMore,
                        isLoadingMore,
                        loadMoreError
                    ) {
                        if (
                            hasMore &&
                            !isLoadingMore &&
                            loadMoreError == null &&
                            latestOnLoadMore != null
                        ) {
                            snapshotFlow {
                                val layoutInfo = gridState.layoutInfo
                                val lastVisibleIndex =
                                    layoutInfo.visibleItemsInfo
                                        .lastOrNull()?.index ?: -1

                                layoutInfo.totalItemsCount > 0 &&
                                        lastVisibleIndex >=
                                        layoutInfo.totalItemsCount - 4
                            }.first { nearBottom -> nearBottom }

                            latestOnLoadMore?.invoke()
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = screenPadding,
                            end = screenPadding,
                            bottom = 24.dp
                        ),
                        horizontalArrangement =
                            Arrangement.spacedBy(gridSpacing),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = movies,
                            key = { movie -> movie.id }
                        ) { movie ->
                            MovieCard(
                                movie = movie,
                                fillGridCell = true,
                                onClick = {
                                    onMovieClick(movie)
                                }
                            )
                        }

                        // Error while loading movies
                        if (loadMoreError != null) {
                            item(
                                span = {
                                    GridItemSpan(maxLineSpan)
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = loadMoreError,
                                        color = MovaraText
                                    )

                                    if (onRetry != null) {
                                        Spacer(
                                            Modifier.height(8.dp)
                                        )

                                        TextButton(
                                            onClick = onRetry
                                        ) {
                                            Text(
                                                text = stringResource(R.string.selected_genre_retry)
                                            )
                                        }
                                    }
                                }
                            }
                        }


// Show poster placeholders while loading the next page
                        if (isLoadingMore) {
                            items(columns) {
                                PulsingPlaceholder(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(144f / 208f)
                                        .testTag("genre_loading_more_placeholder"),
                                    cornerRadius = 12.dp
                                )
                            }
                        }

                    }
                }
            }
        }
    }
}

// Preview-only data, not used for real TMDB requests
private val sampleMovies = (1..20).map { id ->
    Movie(
        id = id,
        title = "Sample Movie $id",
        posterPath = null,
        releaseYear = 2025
    )
}

@Preview(
    name = "Phone - 3 columns",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun PhonePreview() {
    MovaraTheme {
        SelectedGenreScreen(
            genreId = 28,
            movies = sampleMovies,
            onBack = {},
            onMovieClick = {}
        )
    }
}

@Preview(
    name = "Medium - 5 columns",
    showBackground = true,
    widthDp = 700,
    heightDp = 900
)
@Composable
private fun MediumPreview() {
    MovaraTheme {
        SelectedGenreScreen(
            genreId = 35,
            movies = sampleMovies,
            onBack = {},
            onMovieClick = {}
        )
    }
}

@Preview(
    name = "Loading",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun LoadingPreview() {
    MovaraTheme {
        SelectedGenreScreen(
            genreId = 28,
            movies = emptyList(),
            isLoading = true,
            onBack = {},
            onMovieClick = {}
        )
    }
}

@Preview(
    name = "Error",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun ErrorPreview() {
    MovaraTheme {
        SelectedGenreScreen(
            genreId = 28,
            movies = emptyList(),
            errorMessage = "Could not load movies.",
            onRetry = {},
            onBack = {},
            onMovieClick = {}
        )
    }
}

@Preview(
    name = "Empty",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun EmptyPreview() {
    MovaraTheme {
        SelectedGenreScreen(
            genreId = 28,
            movies = emptyList(),
            onBack = {},
            onMovieClick = {}
        )
    }
}
