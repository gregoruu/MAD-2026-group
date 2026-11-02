package ee.movara.app.ui.theme

import androidx.compose.ui.text.TextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the rule behind filling all fifteen Material 3 slots in [Typography]: a slot left unset
 * falls back to the system font, which shows up as Roboto in a screen that should be Barlow
 * Condensed or Inter. A slot added later without a font family fails here instead of in review.
 */
class TypographyTest {

  private val allSlots: List<Pair<String, TextStyle>> =
    listOf(
      "displayLarge" to Typography.displayLarge,
      "displayMedium" to Typography.displayMedium,
      "displaySmall" to Typography.displaySmall,
      "headlineLarge" to Typography.headlineLarge,
      "headlineMedium" to Typography.headlineMedium,
      "headlineSmall" to Typography.headlineSmall,
      "titleLarge" to Typography.titleLarge,
      "titleMedium" to Typography.titleMedium,
      "titleSmall" to Typography.titleSmall,
      "bodyLarge" to Typography.bodyLarge,
      "bodyMedium" to Typography.bodyMedium,
      "bodySmall" to Typography.bodySmall,
      "labelLarge" to Typography.labelLarge,
      "labelMedium" to Typography.labelMedium,
      "labelSmall" to Typography.labelSmall,
    )

  @Test
  fun everySlotUsesAMovaraFontFamily() {
    val wrong = allSlots.filter { (_, style) -> style.fontFamily !in listOf(BarlowCondensed, Inter) }
    assertTrue("Slots without a Movara font: ${wrong.map { it.first }}", wrong.isEmpty())
  }

  @Test
  fun noTextIsSmallerThanTheSpecMinimum() {
    // docs/design-specs.md section 2: "The smallest text in the app is 12 sp."
    val tooSmall = allSlots.filter { (_, style) -> style.fontSize.value < 12f }
    assertTrue("Slots below 12 sp: ${tooSmall.map { it.first }}", tooSmall.isEmpty())
  }

  @Test
  fun specifiedStylesMatchTheDesignSpec() {
    // Only the styles docs/design-specs.md section 2 names explicitly.
    assertSizeAndHeight(Typography.displayLarge, 48f, 48f)
    assertSizeAndHeight(Typography.displayMedium, 36f, 40f)
    assertSizeAndHeight(Typography.headlineMedium, 24f, 24f)
    assertSizeAndHeight(Typography.titleLarge, 20f, 28f)
    assertSizeAndHeight(Typography.titleMedium, 18f, 22f)
    assertSizeAndHeight(Typography.titleSmall, 14f, 18f)
    assertSizeAndHeight(Typography.bodyLarge, 14f, 20f)
    assertSizeAndHeight(MovaraTextStyles.bodyRelaxed, 14f, 23f)
    assertSizeAndHeight(Typography.labelLarge, 14f, 20f)
    assertSizeAndHeight(Typography.labelMedium, 12f, 16f)
  }

  private fun assertSizeAndHeight(style: TextStyle, size: Float, lineHeight: Float) {
    assertEquals(size, style.fontSize.value, 0.01f)
    assertEquals(lineHeight, style.lineHeight.value, 0.01f)
  }
}
