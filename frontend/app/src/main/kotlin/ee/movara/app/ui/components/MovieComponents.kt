
package ee.movara.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ee.movara.app.domain.model.Genre
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.theme.MovaraBackground
import ee.movara.app.ui.theme.MovaraBorder
import ee.movara.app.ui.theme.MovaraGold
import ee.movara.app.ui.theme.MovaraSurface
import ee.movara.app.ui.theme.MovaraText
import ee.movara.app.ui.theme.MovaraTextSecondary
import ee.movara.app.ui.theme.*
import androidx.compose.foundation.Image

//Converts a TMDB poster path into a complete image URL.
private fun posterUrl(path: String, size: String): String {
    if (path.startsWith("https://") || path.startsWith("http://")) {
        return path
    }
    val normalized = if (path.startsWith("/")) path else "/$path"
    return "https://image.tmdb.org/t/p/$size$normalized"
}

//Displays a fallback icon when a movie poster is missing or cannot be loaded.
@Composable
private fun MissingPoster(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MovaraSurfaceSecondary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ui_movie),
            contentDescription = null,
            tint = MovaraTextSecondary,
            modifier = Modifier.size(32.dp)
        )
    }
}

// Reusable poster image component.
@Composable
private fun PosterImage(
    path: String?,
    modifier: Modifier = Modifier,
    size: String = "w342"
) {
    Box(modifier = modifier.background(MovaraSurfaceSecondary)) {
        if (path.isNullOrBlank()) {
            MissingPoster(Modifier.matchParentSize())
        } else {
            SubcomposeAsyncImage(
                model = posterUrl(path, size),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                loading = {
                    PulsingPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 0.dp
                    )
                },
                error = {
                    MissingPoster(Modifier.fillMaxSize())
                }
            )
        }
    }
}

// MovieCard: 144 x 208 dp
@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(144.dp).height(208.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MovaraSurface
        ),
        border = BorderStroke(1.dp, MovaraBorder)
    ) {
        Box(Modifier.fillMaxSize()) {
            TmdbImage(
                movie.posterPath,
                modifier = Modifier.matchParentSize(),
                size = "w342"
            )

            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.42f to Color.Transparent,
                        1f to MovaraBackground
                    )
                )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MovaraText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                movie.releaseYear?.let { year ->
                    Text(
                        text = "$year",
                        style = MaterialTheme.typography.labelSmall,
                        color = MovaraTextSecondary
                    )
                }
            }
        }
    }
}

// SpotlightCard: 62% of screen height
@Composable
fun SpotlightCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp? = null
) {
    val targetHeight = height
        ?: (LocalConfiguration.current.screenHeightDp.dp * 0.62f)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(targetHeight),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MovaraSurface
        ),
        border = BorderStroke(1.dp, MovaraBorder)
    ) {
        Box(Modifier.fillMaxSize()) {
            TmdbImage(
                movie.posterPath,
                modifier = Modifier.matchParentSize(),
                size = "w500"
            )

            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to Color.Transparent,
                        1f to MovaraBackground
                    )
                )
            )

            Text(
                text = "TOP PICK",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp),
                color = MovaraGold,
                style = MaterialTheme.typography.labelMedium
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.displayLarge,
                    color = MovaraText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                movie.releaseYear?.let { year ->
                    Text(
                        text = "$year",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MovaraTextSecondary
                    )
                }

                Text(
                    text = "SEE DETAILS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MovaraGold
                )
            }
        }
    }
}

// GenreTile: 112 dp high

@Composable
fun GenreTile(
    genre: Genre,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageRes: Int? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MovaraSurface
        ),
        border = BorderStroke(1.dp, MovaraBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            MovieImage(
                model = imageUrl?.trim()?.takeIf { it.isNotEmpty() },
                modifier = Modifier.matchParentSize()
            )
            // Prefer local images so genre tiles also work offline.
            when {
                imageRes != null -> {
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }

                !imageUrl.isNullOrBlank() -> {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            // Dark overlay keeps the genre name readable.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to MovaraBackground.copy(alpha = 0.10f),
                            1f to MovaraBackground.copy(alpha = 0.85f)
                        )
                    )
            )

            Text(
                text = genre.name,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MovaraText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// MovieRow: 64 x 96 dp poster
@Composable
fun MovieRow(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MovaraSurface
        ),
        border = BorderStroke(1.dp, MovaraBorder)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TmdbImage(
                movie.posterPath,
                modifier = Modifier.size(
                    width = 64.dp,
                    height = 96.dp
                ),
                size = "w342"
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MovaraText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                movie.releaseYear?.let { year ->
                    Text(
                        text = "$year",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaraTextSecondary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))
        }
    }
}

