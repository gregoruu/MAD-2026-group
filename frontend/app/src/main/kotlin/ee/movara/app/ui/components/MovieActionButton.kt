
package ee.movara.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ee.movara.app.R
import ee.movara.app.ui.theme.*

// Defines the available actions a user can perform on a movie.
enum class MovieAction {
    LIKE,
    WATCH_LATER,
    WATCHED
}

// Reusable action button for movie interactions.
//The button changes its icon, text and color depending on the selected action and its current state.
@Composable
fun MovieActionButton(
    action: MovieAction,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val selectedColor: Color = when (action) {
        MovieAction.LIKE -> MovaraLiked
        MovieAction.WATCH_LATER -> MovaraGold
        MovieAction.WATCHED -> MovaraWatched
    }

    val label = when (action) {
        MovieAction.LIKE ->
            if (isSelected) "Liked" else "Like"

        MovieAction.WATCH_LATER ->
            if (isSelected) "Saved" else "Watch Later"

        MovieAction.WATCHED ->
            if (isSelected) "Watched" else "Mark Watched"
    }

    val icon = when (action) {
        MovieAction.LIKE ->
            if (isSelected) R.drawable.ic_ui_favorite_filled
            else R.drawable.ic_ui_favorite_outline

        MovieAction.WATCH_LATER ->
            if (isSelected) R.drawable.ic_ui_bookmark_filled
            else R.drawable.ic_ui_bookmark_outline

        MovieAction.WATCHED ->
            if (isSelected) R.drawable.ic_ui_check
            else R.drawable.ic_ui_visibility
    }

    val containerColor = if (isSelected) selectedColor else MovaraSurfaceSecondary
    val contentColor = if (isSelected) MovaraBackground else MovaraTextSecondary

    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = PillShape,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 10.dp
        ),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        )
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}
