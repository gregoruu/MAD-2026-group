
package ee.movara.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ee.movara.app.ui.theme.MovaraGold
import ee.movara.app.ui.theme.MovaraSurfaceSecondary
import ee.movara.app.ui.theme.MovaraText
import ee.movara.app.ui.theme.MovaraTextSecondary

@Composable
fun MovaraSnackbar(data: SnackbarData) {
    var appeared by remember(data) {
        mutableStateOf(false)
    }

    val offsetY by animateDpAsState(
        targetValue = if (appeared) 0.dp else 64.dp,
        animationSpec = tween(durationMillis = 250),
        label = "snackbar_slide_up"
    )

    LaunchedEffect(data) {
        appeared = true
    }

    Snackbar(
        snackbarData = data,
        modifier = Modifier.offset(y = offsetY),
        shape = RoundedCornerShape(12.dp),
        containerColor = MovaraSurfaceSecondary,
        contentColor = MovaraText,
        actionColor = MovaraGold,
        dismissActionContentColor = MovaraTextSecondary
    )
}
