package ee.movara.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import ee.movara.app.ui.theme.MovaraScrim

// Background for a text block drawn on top of an image.
// The whole block sits on the full scrim, so the text keeps its contrast however
// tall the block grows. The modifier adds the room for the fade above the block
// itself, so the scrim stays inside its own bounds and the caller adds nothing.
internal fun Modifier.textScrim(fade: Dp): Modifier {
    val scrim = this.drawWithCache {
        // Past the end of the gradient the brush stays at the full scrim colour.
        val brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, MovaraScrim),
            startY = 0f,
            endY = fade.toPx()
        )

        onDrawBehind {
            drawRect(brush = brush)
        }
    }
    return scrim.padding(top = fade)
}
