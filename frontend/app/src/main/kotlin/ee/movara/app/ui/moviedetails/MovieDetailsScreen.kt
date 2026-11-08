
package ee.movara.app.ui.moviedetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.movara.app.R
import ee.movara.app.domain.model.Genre
import ee.movara.app.domain.model.MovieDetails
import ee.movara.app.ui.components.MovieAction
import ee.movara.app.ui.components.MovieActionButton
import ee.movara.app.ui.components.TmdbImage
import ee.movara.app.ui.theme.*
import java.util.Locale

@Composable
fun MovieDetailsScreen(
    movie: MovieDetails,
    isLiked: Boolean,
    isWatchLater: Boolean,
    isWatched: Boolean,
    onLikeClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MovaraBackground)
    ) {
        val isMediumScreen = maxWidth >= 600.dp
        val screenPadding = if (isMediumScreen) 24.dp else 20.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (isMediumScreen) {
                // Poster and information side by side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = screenPadding,
                            vertical = 16.dp
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    MovieHero(
                        movie = movie,
                        onBack = onBack,
                        usePoster = true,
                        modifier = Modifier
                            .width(280.dp)
                            .height(420.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    MovieInformation(
                        movie = movie,
                        isLiked = isLiked,
                        isWatchLater = isWatchLater,
                        isWatched = isWatched,
                        onLikeClick = onLikeClick,
                        onWatchLaterClick = onWatchLaterClick,
                        onWatchedClick = onWatchedClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Image, back button and title in one
                MovieHero(
                    movie = movie,
                    onBack = onBack,
                    usePoster = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )

                MovieInformation(
                    movie = movie,
                    isLiked = isLiked,
                    isWatchLater = isWatchLater,
                    isWatched = isWatched,
                    onLikeClick = onLikeClick,
                    onWatchLaterClick = onWatchLaterClick,
                    onWatchedClick = onWatchedClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = screenPadding,
                            vertical = 16.dp
                        )
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// Shared section with image, gradient, back button and title
@Composable
private fun MovieHero(
    movie: MovieDetails,
    onBack: () -> Unit,
    usePoster: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        TmdbImage(
            path = if (usePoster) {
                movie.posterPath ?: movie.backdropPath
            } else {
                movie.backdropPath ?: movie.posterPath
            },
            modifier = Modifier.matchParentSize(),
            size = "w780"
        )

        // Gradient keeps the title readable
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            MovaraBackground.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(48.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.35f),
                    shape = CircleShape
                )
        ) {
            Icon(
                painter = painterResource(
                    R.drawable.ic_ui_arrow_back
                ),
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Title overlaps the lower part of the image
        Text(
            text = movie.title,
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
            maxLines = if (usePoster) 4 else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    horizontal = if (usePoster) 16.dp else 20.dp,
                    vertical = 16.dp
                )
        )
    }
}

@Composable
private fun MovieInformation(
    movie: MovieDetails,
    isLiked: Boolean,
    isWatchLater: Boolean,
    isWatched: Boolean,
    onLikeClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onWatchedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Year, runtime and rating
        val metadata = buildList {
            movie.releaseYear?.let { year ->
                add(year.toString())
            }

            movie.runtimeMinutes
                ?.takeIf { it > 0 }
                ?.let { minutes ->
                    add("$minutes min")
                }

            movie.rating?.let { rating ->
                add(
                    "★ " + String.format(
                        Locale.US,
                        "%.1f",
                        rating
                    )
                )
            }
        }

        if (metadata.isNotEmpty()) {
            Text(
                text = metadata.joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = MovaraTextSecondary
            )
        }

        // Genres
        if (movie.genres.isNotEmpty()) {
            Text(
                text = movie.genres.joinToString("  ·  ") {
                    it.name
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MovaraGold
            )
        }

        // Director
        movie.director
            ?.takeIf { it.isNotBlank() }
            ?.let { director ->
                Text(
                    text = "Director: $director",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MovaraText
                )
            }

        // Compact action buttons that wrap when needed
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            MovieActionButton(
                action = MovieAction.LIKE,
                isSelected = isLiked,
                onClick = onLikeClick
            )

            MovieActionButton(
                action = MovieAction.WATCH_LATER,
                isSelected = isWatchLater,
                onClick = onWatchLaterClick
            )

            MovieActionButton(
                action = MovieAction.WATCHED,
                isSelected = isWatched,
                onClick = onWatchedClick
            )
        }

        // Synopsis
        Text(
            text = "Synopsis",
            style = MaterialTheme.typography.titleLarge,
            color = MovaraText
        )

        Text(
            text = movie.overview
                ?.takeIf { it.isNotBlank() }
                ?: "No description available",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 23.sp,
            color = MovaraTextSecondary
        )

        // First five actors
        val actors = movie.topCast
            .filter { it.isNotBlank() }
            .take(5)

        if (actors.isNotEmpty()) {
            Text(
                text = "Cast",
                style = MaterialTheme.typography.titleLarge,
                color = MovaraText
            )

            Text(
                text = actors.joinToString("\n"),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 23.sp,
                color = MovaraTextSecondary
            )
        }
    }
}

// Preview data
private val previewMovie = MovieDetails(
    id = 1,
    title = "Midnight Orbit",
    releaseYear = 2025,
    runtimeMinutes = 128,
    rating = 7.6,
    genres = listOf(
        Genre(878, "Science Fiction"),
        Genre(28, "Action")
    ),
    director = "Alex Morgan",
    overview = "An astronaut discovers a mysterious signal " +
            "during a journey beyond the solar system.",
    posterPath = null,
    backdropPath = null,
    topCast = listOf(
        "Jordan Lee",
        "Taylor Brooks",
        "Sam Rivera",
        "Morgan Blake",
        "Jamie Ellis",
        "Casey Gray"
    )
)

@Preview(
    name = "Phone - Movie Details",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun MovieDetailsPhonePreview() {
    MovaraTheme {
        var liked by remember { mutableStateOf(false) }
        var watchLater by remember { mutableStateOf(false) }
        var watched by remember { mutableStateOf(false) }

        MovieDetailsScreen(
            movie = previewMovie,
            isLiked = liked,
            isWatchLater = watchLater,
            isWatched = watched,
            onLikeClick = {
                liked = !liked
            },
            onWatchLaterClick = {
                watchLater = !watchLater
            },
            onWatchedClick = {
                watched = !watched
                if (watched) watchLater = false
            },
            onBack = {}
        )
    }
}

@Preview(
    name = "Medium - Movie Details",
    showBackground = true,
    widthDp = 700,
    heightDp = 900
)
@Composable
private fun MovieDetailsMediumPreview() {
    MovaraTheme {
        MovieDetailsScreen(
            movie = previewMovie,
            isLiked = true,
            isWatchLater = false,
            isWatched = false,
            onLikeClick = {},
            onWatchLaterClick = {},
            onWatchedClick = {},
            onBack = {}
        )
    }
}

@Preview(
    name = "Missing optional data",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun MovieDetailsMissingDataPreview() {
    MovaraTheme {
        MovieDetailsScreen(
            movie = previewMovie.copy(
                runtimeMinutes = null,
                overview = null,
                director = null,
                topCast = emptyList()
            ),
            isLiked = false,
            isWatchLater = false,
            isWatched = false,
            onLikeClick = {},
            onWatchLaterClick = {},
            onWatchedClick = {},
            onBack = {}
        )
    }
}
