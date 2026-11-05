
package ee.movara.app.ui.genres

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.movara.app.domain.model.Genre
import ee.movara.app.ui.components.GenreTile
import ee.movara.app.ui.theme.MovaraBackground
import ee.movara.app.ui.theme.MovaraTheme
import ee.movara.app.R

// Fixed list of ten genres with their TMDB IDs.
private val genres = listOf(
    Genre(28, "Action"),
    Genre(18, "Drama"),
    Genre(878, "Sci-Fi"),
    Genre(53, "Thriller"),
    Genre(27, "Horror"),
    Genre(35, "Comedy"),
    Genre(10749, "Romance"),
    Genre(80, "Crime"),
    Genre(16, "Animation"),
    Genre(99, "Documentary")
)

/**
 * Displays ten movie genres in a responsive grid.
 * Selecting a genre passes its TMDB ID to navigation.
 */
@Composable
fun GenresScreen(
    onGenreClick: (Int) -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MovaraBackground)
    ) {
        val columns = if (maxWidth >= 600.dp) 3 else 2

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = genres,
                key = { it.id }
            ) { genre ->

                GenreTile(
                    genre = genre,
                    imageUrl = null,
                    imageRes = when (genre.id) {
                        28 -> R.drawable.genre_action
                        18 -> R.drawable.genre_drama
                        878 -> R.drawable.genre_scifi
                        53 -> R.drawable.genre_thriller
                        27 -> R.drawable.genre_horror
                        35 -> R.drawable.genre_comedy
                        10749 -> R.drawable.genre_romance
                        80 -> R.drawable.genre_crime
                        16 -> R.drawable.genre_animation
                        99 -> R.drawable.genre_documentary
                        else -> null
                    },
                    onClick = {
                        onGenreClick(genre.id)
                    }
                )

            }
        }
    }
}

/**
 * Preview of the Genres screen.
 */
@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun GenresScreenPreview() {
    MovaraTheme {
        GenresScreen()
    }
}
