package ee.movara.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Corner radii from docs/design-specs.md section 3.
val MovaraShapes =
  Shapes(
    // Smallest step of the 4 dp scale; the spec lists no element at this radius yet.
    extraSmall = RoundedCornerShape(4.dp),
    // Posters and tabs.
    small = RoundedCornerShape(8.dp),
    // Cards, genre tiles and list rows.
    medium = RoundedCornerShape(12.dp),
    // Search field.
    large = RoundedCornerShape(16.dp),
    // Spotlight card on Home.
    extraLarge = RoundedCornerShape(24.dp),
  )

/** Pills and round buttons. The spec calls this radius "full", which Material 3 has no slot for. */
val PillShape = CircleShape
