package ee.movara.app.ui.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ee.movara.app.ui.theme.MovaraSpacing

/**
 * Layout values that differ between the two designed screen sizes, from
 * docs/design-specs.md section 6. Screens read these instead of measuring their own width,
 * so every screen switches at the same moment as the navigation.
 */
data class ScreenLayout(
  /** Navigation rail, wide Spotlight card and two-column Movie Details. */
  val isMedium: Boolean,
  val screenMargin: Dp,
  /** Search results and the movies of a selected genre. */
  val movieGridColumns: Int,
  /** Gap between the posters of a selected genre. */
  val movieGridSpacing: Dp,
  val genreTileColumns: Int,
  val savedMovieColumns: Int
)

val CompactScreenLayout = ScreenLayout(
  isMedium = false,
  screenMargin = MovaraSpacing.screenMargin,
  movieGridColumns = 3,
  movieGridSpacing = 8.dp,
  genreTileColumns = 2,
  savedMovieColumns = 1
)

val MediumScreenLayout = ScreenLayout(
  isMedium = true,
  screenMargin = MovaraSpacing.screenMarginExpanded,
  movieGridColumns = 5,
  movieGridSpacing = 12.dp,
  genreTileColumns = 3,
  savedMovieColumns = 2
)

/**
 * Only two sizes are designed. Windows wider than medium (840 dp and up) reuse the medium
 * layout instead of falling back to the phone one.
 */
fun screenLayoutFor(widthSizeClass: WindowWidthSizeClass): ScreenLayout {
  if (widthSizeClass == WindowWidthSizeClass.Compact) {
    return CompactScreenLayout
  }
  return MediumScreenLayout
}

/** Compact unless MainActivity, a preview or a test provides something else. */
val LocalScreenLayout = compositionLocalOf { CompactScreenLayout }

/**
 * For previews and tests, which have no activity window to measure. Picks the layout from
 * the space the content is given, with the same breakpoints as the real window.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ScreenLayoutForAvailableSize(content: @Composable () -> Unit) {
  BoxWithConstraints {
    val sizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
    val layout = screenLayoutFor(sizeClass.widthSizeClass)
    CompositionLocalProvider(LocalScreenLayout provides layout) {
      content()
    }
  }
}
