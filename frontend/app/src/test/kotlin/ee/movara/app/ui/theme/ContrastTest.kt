package ee.movara.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Holds the palette to the ratios copied from the contrast table in docs/design-specs.md.
 * A palette colour changed later without rechecking the contrast fails here instead of in
 * review. The test does not read the document: a ratio edited there must be edited here too.
 */
class ContrastTest {

  // One row of the contrast table in the spec.
  private class SpecRow(
    val name: String,
    val text: Color,
    val background: Color,
    val ratio: Double,
  )

  // WCAG 2.1 relative luminance of an opaque sRGB colour.
  private fun luminance(color: Color): Double {
    val channels = listOf(color.red, color.green, color.blue)
    val linear = mutableListOf<Double>()
    for (channel in channels) {
      val value = channel.toDouble()
      if (value <= 0.03928) {
        linear.add(value / 12.92)
      } else {
        linear.add(((value + 0.055) / 1.055).pow(2.4))
      }
    }
    return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]
  }

  private fun contrast(text: Color, background: Color): Double {
    val first = luminance(text)
    val second = luminance(background)
    val lighter = max(first, second)
    val darker = min(first, second)
    return (lighter + 0.05) / (darker + 0.05)
  }

  // Every row has the ratio from the spec and passes AA for normal text.
  private fun assertRowsMatchTheSpec(rows: List<SpecRow>) {
    for (row in rows) {
      val ratio = contrast(row.text, row.background)
      assertEquals("${row.name}: contrast differs from the spec table", row.ratio, ratio, 0.1)
      assertTrue("${row.name}: contrast $ratio is below WCAG AA for normal text", ratio >= 4.5)
    }
  }

  @Test
  fun palettePairs_matchTheContrastTableInTheSpec() {
    val rows = listOf(
      SpecRow("text on background", MovaraText, MovaraBackground, 17.2),
      SpecRow("secondary text on background", MovaraTextSecondary, MovaraBackground, 8.2),
      SpecRow("gold on background", MovaraGold, MovaraBackground, 10.7),
      SpecRow("background on gold", MovaraBackground, MovaraGold, 10.7),
      SpecRow("background on liked", MovaraBackground, MovaraLiked, 4.8),
      SpecRow("background on watched", MovaraBackground, MovaraWatched, 5.0),
    )

    assertRowsMatchTheSpec(rows)
  }

  // The colour seen when a half-transparent colour is drawn over an opaque one.
  private fun over(top: Color, bottom: Color): Color {
    val alpha = top.alpha
    return Color(
      red = top.red * alpha + bottom.red * (1f - alpha),
      green = top.green * alpha + bottom.green * (1f - alpha),
      blue = top.blue * alpha + bottom.blue * (1f - alpha),
    )
  }

  // Checks the scrim colour only. That the text really sits on the full scrim is
  // the job of Modifier.textScrim, which keeps the fade above the text block.
  @Test
  fun textOnTheImageScrim_matchesTheSpecEvenOnAWhiteImage() {
    // White is the worst case for light text on a poster.
    val scrimOnWhite = over(MovaraScrim, Color.White)
    val rows = listOf(
      SpecRow("text on image scrim", MovaraText, scrimOnWhite, 11.8),
      SpecRow("secondary text on image scrim", MovaraTextSecondary, scrimOnWhite, 5.6),
      SpecRow("gold on image scrim", MovaraGold, scrimOnWhite, 7.3),
    )

    assertRowsMatchTheSpec(rows)
  }

  @Test
  fun textOnCardsAndInactiveButtons_meetsAaForNormalText() {
    // Pairs the app uses that the spec table does not list.
    val names = listOf(
      "text on surface",
      "secondary text on surface",
      "gold on surface",
      "text on secondary surface",
      "secondary text on secondary surface",
    )
    val pairs = listOf(
      MovaraText to MovaraSurface,
      MovaraTextSecondary to MovaraSurface,
      MovaraGold to MovaraSurface,
      MovaraText to MovaraSurfaceSecondary,
      MovaraTextSecondary to MovaraSurfaceSecondary,
    )

    for (index in pairs.indices) {
      val pair = pairs[index]
      val ratio = contrast(pair.first, pair.second)
      assertTrue("${names[index]}: contrast $ratio is below WCAG AA for normal text", ratio >= 4.5)
    }
  }
}
