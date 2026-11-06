package ee.movara.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import ee.movara.app.R
import ee.movara.app.ui.theme.MovaraSurfaceSecondary
import ee.movara.app.ui.theme.MovaraTextSecondary

private fun posterUrl(path: String, size: String): String {
    if (path.startsWith("https://") || path.startsWith("http://")) {
        return path
    }

    val normalized = if (path.startsWith("/")) path else "/$path"
    return "https://image.tmdb.org/t/p/$size$normalized"
}

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

@Composable
fun TmdbImage(
    path: String?,
    modifier: Modifier = Modifier,
    size: String = "w342"
) {
    val normalizedPath = path?.trim()?.takeIf { it.isNotEmpty() }
    MovieImage(
        model = normalizedPath?.let { posterUrl(it, size) },
        modifier = modifier
    )
}

/** Shared rendering for TMDB images and genre image URLs, without subcomposition. */
@Composable
internal fun MovieImage(
    model: Any?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(MovaraSurfaceSecondary)) {
        if (model == null) {
            MissingPoster(Modifier.matchParentSize())
        } else {
            // Reset loading/error state when a recycled card receives another image.
            key(model) {
                var state by remember {
                    mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
                }

                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                    onState = { state = it }
                )

                when (state) {
                    is AsyncImagePainter.State.Empty,
                    is AsyncImagePainter.State.Loading -> PulsingPlaceholder(
                        modifier = Modifier.matchParentSize(),
                        cornerRadius = 0.dp
                    )
                    is AsyncImagePainter.State.Error ->
                        MissingPoster(Modifier.matchParentSize())
                    is AsyncImagePainter.State.Success -> Unit
                }
            }
        }
    }
}
