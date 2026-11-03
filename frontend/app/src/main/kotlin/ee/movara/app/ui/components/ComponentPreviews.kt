
package ee.movara.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.movara.app.domain.model.Genre
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.theme.*

// Compose previews for the shared UI components to help out.
private val previewMovie = Movie(
    id = 1,
    title = "Interstellar",
    posterPath = null,
    releaseYear = 2014
)

@Preview(
    name = "MovieCard",
    widthDp = 180,
    heightDp = 260,
    showBackground = true
)
@Composable
private fun MovieCardPreview() {
    MovaraTheme {
        Column(
            Modifier.background(MovaraBackground).padding(16.dp)
        ) {
            MovieCard(
                movie = previewMovie,
                onClick = {}
            )
        }
    }
}

@Preview(
    name = "SpotlightCard",
    widthDp = 412,
    heightDp = 440,
    showBackground = true
)
@Composable
private fun SpotlightCardPreview() {
    MovaraTheme {
        Column(
            Modifier.background(MovaraBackground).padding(16.dp)
        ) {
            SpotlightCard(
                movie = previewMovie,
                height = 380.dp,
                onClick = {}
            )
        }
    }
}

@Preview(
    name = "GenreTile",
    widthDp = 220,
    heightDp = 150,
    showBackground = true
)
@Composable
private fun GenreTilePreview() {
    MovaraTheme {
        Column(
            Modifier.background(MovaraBackground).padding(16.dp)
        ) {
            GenreTile(
                genre = Genre(28, "Action"),
                imageUrl = null,
                onClick = {}
            )
        }
    }
}

@Preview(
    name = "MovieRow",
    widthDp = 412,
    heightDp = 140,
    showBackground = true
)
@Composable
private fun MovieRowPreview() {
    MovaraTheme {
        Column(
            Modifier.background(MovaraBackground).padding(16.dp)
        ) {
            MovieRow(
                movie = previewMovie,
                onClick = {}
            )
        }
    }
}

@Preview(
    name = "Action buttons: off and on",
    widthDp = 400,
    heightDp = 345,
    showBackground = true
)
@Composable
private fun ActionButtonsPreview() {
    MovaraTheme {
        Column(
            modifier = Modifier
                .background(MovaraBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MovieAction.entries.forEach { action ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MovieActionButton(
                        action = action,
                        isSelected = false,
                        onClick = {}
                    )

                    MovieActionButton(
                        action = action,
                        isSelected = true,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Search field",
    widthDp = 412,
    heightDp = 100,
    showBackground = true
)
@Composable
private fun SearchFieldPreview() {
    MovaraTheme {
        var query by remember {
            mutableStateOf("Movie")
        }

        Column(
            Modifier.background(MovaraBackground).padding(16.dp)
        ) {
            MovieSearchField(
                query = query,
                onQueryChange = { query = it }
            )
        }
    }
}

@Preview(
    name = "Pulsing placeholder",
    widthDp = 412,
    heightDp = 280,
    showBackground = true
)
@Composable
private fun PulsingPlaceholderPreview() {
    MovaraTheme {
        Column(
            modifier = Modifier
                .background(MovaraBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PulsingPlaceholder(
                    Modifier.size(144.dp, 208.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PulsingPlaceholder(
                        Modifier.size(160.dp, 24.dp),
                        cornerRadius = 6.dp
                    )

                    PulsingPlaceholder(
                        Modifier.size(100.dp, 16.dp),
                        cornerRadius = 6.dp
                    )
                }
            }
        }
    }
}
