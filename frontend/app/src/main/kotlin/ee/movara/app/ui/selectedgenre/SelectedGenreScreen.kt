
package ee.movara.app.ui.selectedgenre

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.movara.app.R
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.components.MovieCard
import ee.movara.app.ui.theme.*

// Returns the name of the selected TMDB genre
private fun genreName(genreId: Int): String {
    return when (genreId) {
        28 -> "Action"
        18 -> "Drama"
        878 -> "Sci-Fi"
        53 -> "Thriller"
        27 -> "Horror"
        35 -> "Comedy"
        10749 -> "Romance"
        80 -> "Crime"
        16 -> "Animation"
        99 -> "Documentary"
        else -> "Movies"
    }
}

//Displays movies belonging to the selected genre. Data and loading states are provided by the caller.

@Composable
fun SelectedGenreScreen(
    genreId: Int,
    movies: List<Movie>,
    onBack: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MovaraBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(
                        R.drawable.ic_ui_arrow_back
                    ),
                    contentDescription = "Back",
                    tint = MovaraText
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = genreName(genreId),
                style = MaterialTheme.typography.titleLarge,
                color = MovaraGold
            )
        }

        when {
            // Loading state
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MovaraGold
                    )
                }
            }

            // Error state
            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage,
                        color = MovaraText,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(onClick = onRetry) {
                        Text("Try again")
                    }
                }
            }

            // Empty state
            movies.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No movies found for this genre.",
                        color = MovaraTextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Grid
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(
                        minSize = 150.dp
                    ),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {
                    items(movies) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = {
                                onMovieClick(movie)
                            }
                        )
                    }
                }
            }
        }
    }
}

// Preview of the selected genre screen.
@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun SelectedGenreScreenPreview() {
    MovaraTheme {
        SelectedGenreScreen(
            genreId = 28,
            movies = emptyList(),
            onBack = {},
            onMovieClick = {}
        )
    }
}


/**
 * Sample movie data used only for UI previews.
 * Real movie data will be provided by MovieRepository.
 */
private val sampleMovies = listOf(
    Movie(
        id = 1,
        title = "Sample Action Movie",
        posterPath = null,
        releaseYear = 2025
    ),
    Movie(
        id = 2,
        title = "Another Movie",
        posterPath = null,
        releaseYear = 2024
    ),
    Movie(
        id = 3,
        title = "Movie with a Longer Title",
        posterPath = null,
        releaseYear = 2023
    ),
    Movie(
        id = 4,
        title = "Fourth Movie",
        posterPath = null,
        releaseYear = 2022
    ),
    Movie(
        id = 5,
        title = "Fifth Movie",
        posterPath = null,
        releaseYear = null
    ),
    Movie(
        id = 6,
        title = "Sixth Movie",
        posterPath = null,
        releaseYear = 2021
    )
)
@Preview(
    name = "Movies loaded",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun SelectedGenreMoviesPreview() {
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
    name = "Loading",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun SelectedGenreLoadingPreview() {
    MovaraTheme {
        SelectedGenreScreen(
            genreId = 35,
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
private fun SelectedGenreErrorPreview() {
    MovaraTheme {
        SelectedGenreScreen(
            genreId = 18,
            movies = emptyList(),
            errorMessage = "Could not load movies.",
            onBack = {},
            onMovieClick = {},
            onRetry = {}
        )
    }
}

