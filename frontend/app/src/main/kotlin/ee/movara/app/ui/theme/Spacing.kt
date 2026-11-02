package ee.movara.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale from docs/design-specs.md section 3. Every value is a multiple of 4 dp.
 *
 * Element sizes that belong to one component only, such as the 144 x 208 dp movie card or the
 * 52 dp search field, stay with those components in the shared components ticket (#31).
 */
object MovaraSpacing {

  /** Base step of the scale. The spec lists no element at this size yet. */
  val tiny = 4.dp

  /** Gap between chips. */
  val small = 8.dp

  /** Gap between movie cards in rails and genre tiles, and between action buttons. */
  val medium = 12.dp

  /** Gap between movie cards in grids. */
  val large = 16.dp

  /** Screen margin on phones (compact width). */
  val screenMargin = 20.dp

  /** Screen margin on foldables and small tablets (medium width). */
  val screenMarginExpanded = 24.dp

  /** Gap between sections on Home. */
  val section = 40.dp

  /**
   * Smallest allowed size for anything clickable, from the accessibility rules in section 10.
   * Buttons, tabs and list rows are at least this tall.
   */
  val minimumTouchTarget = 48.dp
}
