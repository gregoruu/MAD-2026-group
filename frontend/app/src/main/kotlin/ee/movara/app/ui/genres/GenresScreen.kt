
package ee.movara.app.ui.genres

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.movara.app.R
import ee.movara.app.domain.model.Genre
import ee.movara.app.ui.components.GenreTile
import ee.movara.app.ui.theme.*

// Ten predefined genres with their TMDB IDs
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

// Displays movie genres in a responsive grid
// Uses two columns on phones and three on medium screens

@Composable
fun GenresScreen(
    onGenreClick: (Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MovaraBackground)
    ) {
        val isMediumScreen = maxWidth >= 600.dp
        val columns = if (isMediumScreen) 3 else 2
        val screenPadding = if (isMediumScreen) 24.dp else 20.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = screenPadding,
                    end = screenPadding,
                    top = screenPadding
                )
        ) {
            // Required screen heading
            Text(
                text = "Genres",
                style = MaterialTheme.typography.displayMedium,
                color = MovaraText
            )

            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
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
}

@Preview(
    name = "Phone - 2 columns",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun GenresPhonePreview() {
    MovaraTheme {
        GenresScreen(onGenreClick = {})
    }
}

@Preview(
    name = "Medium - 3 columns",
    showBackground = true,
    widthDp = 700,
    heightDp = 900
)
@Composable
private fun GenresMediumPreview() {
    MovaraTheme {
        GenresScreen(onGenreClick = {})
    }
}
