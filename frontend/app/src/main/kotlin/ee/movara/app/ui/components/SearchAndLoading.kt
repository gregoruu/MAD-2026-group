
package ee.movara.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ee.movara.app.R
import ee.movara.app.ui.theme.*


// Reusable search input for finding movies by title.
@Composable
fun MovieSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    var focused by remember { mutableStateOf(false) }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MovaraText
        ),
        cursorBrush = SolidColor(MovaraGold),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(MovaraSurface)
            .border(
                width = 1.dp,
                color = if (focused) MovaraGold else MovaraBorder,
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(16.dp))

                Icon(
                    painter = painterResource(R.drawable.ic_ui_search),
                    contentDescription = null,
                    tint = MovaraTextSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search movies by title",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MovaraTextSecondary,
                            maxLines = 1
                        )
                    }

                    innerTextField()
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ui_close),
                            contentDescription = "Clear search",
                            tint = MovaraTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.width(16.dp))
                }
            }
        }
    )
}

//Displays an animated loading placeholder.
@Composable
fun PulsingPlaceholder(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val transition = rememberInfiniteTransition(
        label = "MovaraLoading"
    )

    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "placeholderAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                MovaraSurfaceSecondary.copy(alpha = alpha)
            )
    )
}
