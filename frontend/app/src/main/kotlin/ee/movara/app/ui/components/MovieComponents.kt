
package ee.movara.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import ee.movara.app.R
import ee.movara.app.domain.model.Genre
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.theme.MovaraBorder
import ee.movara.app.ui.theme.MovaraGold
import ee.movara.app.ui.theme.MovaraSurface
import ee.movara.app.ui.theme.MovaraText
import ee.movara.app.ui.theme.MovaraTextSecondary
import ee.movara.app.ui.theme.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio

//Converts a TMDB poster path into a complete image URL
private fun posterUrl(path: String, size: String): String {
    if (path.startsWith("https://") || path.startsWith("http://")) {
        return path
    }
    val normalized = if (path.startsWith("/")) path else "/$path"
    return "https://image.tmdb.org/t/p/$size$normalized"
}

//Displays a fallback icon when a movie poster is missing or cant be loaded
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

// Reusable poster image
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
    modifier: Modifier = Modifier,
    fillGridCell: Boolean = false
) {
    val cardModifier = if (fillGridCell) {
        modifier
            .fillMaxWidth()
            .aspectRatio(144f / 208f)
    } else {
        modifier
            .width(144.dp)
            .height(208.dp)
    }
    Card(
        onClick = onClick,
        modifier = cardModifier,
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

            // The scrim belongs to the text block, so it covers the title at any font size.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .textScrim(fade = 16.dp)
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

// The phone Spotlight card grows with its text, so a long title gets more lines there.
// The wide card has a fixed height and keeps the title short.
private const val SPOTLIGHT_PHONE_TITLE_MAX_LINES = 4
private const val SPOTLIGHT_WIDE_TITLE_MAX_LINES = 2

// SpotlightCard: 62% of screen height.
// The phone layout grows past that when a large font needs more room.
@Composable
fun SpotlightCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    horizontal: Boolean = false,
    actions: (@Composable () -> Unit)? = null
) {
    val targetHeight = height
        ?: (LocalConfiguration.current.screenHeightDp.dp * 0.62f)

    // The wide layout scrolls inside the card, so it keeps a fixed height.
    // The phone layout sets its own minimum height on the text column below.
    val heightModifier = if (horizontal) {
        Modifier.height(targetHeight)
    } else {
        Modifier
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().then(heightModifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MovaraSurface
        ),
        border = BorderStroke(1.dp, MovaraBorder)
    ) {
        if (horizontal) {
            Row(Modifier.fillMaxSize()) {
                TmdbImage(
                    movie.posterPath,
                    modifier = Modifier.weight(0.4f).fillMaxHeight(),
                    size = "w500"
                )
                Column(
                    modifier = Modifier.weight(0.6f).fillMaxHeight()
                        .verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Plain label: here it sits on the card surface, not on the poster.
                    Text(
                        text = "TOP PICK",
                        color = MovaraGold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    SpotlightText(
                        movie = movie,
                        onClick = onClick,
                        actions = actions,
                        titleMaxLines = SPOTLIGHT_WIDE_TITLE_MAX_LINES
                    )
                }
            }
        } else Box(Modifier.fillMaxWidth()) {
            TmdbImage(
                movie.posterPath,
                modifier = Modifier.matchParentSize(),
                size = "w500"
            )

            // The badge and the text block share one column, so they never overlap
            // when the text block grows.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = targetHeight),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // The badge has its own dark background because the poster behind it is unknown.
                Text(
                    text = "TOP PICK",
                    modifier = Modifier
                        .padding(20.dp)
                        .background(
                            color = MovaraScrim,
                            shape = PillShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    color = MovaraGold,
                    style = MaterialTheme.typography.labelMedium
                )

                // The scrim belongs to the text block and covers it at any font size.
                // The year and the details link sit on the poster too, so they need it as well.
                SpotlightText(
                    movie = movie,
                    onClick = onClick,
                    actions = actions,
                    titleMaxLines = SPOTLIGHT_PHONE_TITLE_MAX_LINES,
                    modifier = Modifier
                        .fillMaxWidth()
                        .textScrim(fade = 64.dp)
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                )
            }
        }
    }
}

// Title, year, action buttons and the details link, shared by both Spotlight layouts.
@Composable
private fun SpotlightText(
    movie: Movie,
    onClick: () -> Unit,
    actions: (@Composable () -> Unit)?,
    titleMaxLines: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = movie.title,
            style = MaterialTheme.typography.displayLarge,
            color = MovaraText,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis
        )

        movie.releaseYear?.let { year ->
            Text(
                text = "$year",
                style = MaterialTheme.typography.bodyLarge,
                color = MovaraTextSecondary
            )
        }

        actions?.invoke()

        androidx.compose.material3.TextButton(
            onClick = onClick,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.home_see_details),
                style = MaterialTheme.typography.labelMedium,
                color = MovaraGold
            )
        }
    }
}

// GenreTile: 112 dp high. With a large font every tile gets the height of a
// two-line name, so the rows of the grid stay even.

// Padding around the genre name and the scrim fade above it; the tile height uses both.
private val GenreNamePadding = 12.dp
private val GenreNameFade = 24.dp

@Composable
fun GenreTile(
    genre: Genre,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageRes: Int? = null
) {
    val nameStyle = MaterialTheme.typography.titleLarge
    // Converted to dp first: font scaling is not linear, so twice the sp value
    // is not twice the height.
    // A style without a line height in sp falls back to the 112 dp minimum.
    val nameLineHeight = if (nameStyle.lineHeight.isSp) {
        with(LocalDensity.current) {
            nameStyle.lineHeight.toDp()
        }
    } else {
        0.dp
    }
    // Two lines of the name, the padding around them and the fade above.
    val tileHeight = maxOf(112.dp, nameLineHeight * 2 + GenreNamePadding * 2 + GenreNameFade)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(tileHeight),
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
            // One answer for both the image below and the scrim behind the name.
            val hasLocalImage = imageRes != null
            val hasRemoteImage = !imageUrl.isNullOrBlank()

            // Prefer local images so genre tiles also work offline.
            when {
                hasLocalImage -> {
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }

                hasRemoteImage -> {
                    MovieImage(
                        model = imageUrl.trim(),
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            // The scrim belongs to the name, so it also covers a two-line name.
            // It is the only dark layer on the image. A tile without an image
            // shows the plain card surface and needs no scrim.
            val scrimModifier = if (hasLocalImage || hasRemoteImage) {
                Modifier.textScrim(fade = GenreNameFade)
            } else {
                Modifier
            }

            Text(
                text = genre.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(scrimModifier)
                    .padding(GenreNamePadding),
                style = nameStyle,
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
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null
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
                actions?.invoke()
            }

            Spacer(Modifier.width(12.dp))
        }
    }
}

