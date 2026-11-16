package ee.movara.app.ui.layout

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the layout values against the table in docs/design-specs.md section 6. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class ScreenLayoutTest {

  private fun layoutForWindow(widthDp: Int, heightDp: Int): ScreenLayout {
    val sizeClass = WindowSizeClass.calculateFromSize(DpSize(widthDp.dp, heightDp.dp))
    return screenLayoutFor(sizeClass.widthSizeClass)
  }

  @Test fun compactTargetUsesPhoneLayout() {
    val layout = layoutForWindow(412, 917)

    assertFalse(layout.isMedium)
    assertEquals(20.dp, layout.screenMargin)
    assertEquals(3, layout.movieGridColumns)
    assertEquals(8.dp, layout.movieGridSpacing)
    assertEquals(2, layout.genreTileColumns)
    assertEquals(1, layout.savedMovieColumns)
  }

  @Test fun mediumTargetUsesMediumLayout() {
    val layout = layoutForWindow(700, 840)

    assertTrue(layout.isMedium)
    assertEquals(24.dp, layout.screenMargin)
    assertEquals(5, layout.movieGridColumns)
    assertEquals(12.dp, layout.movieGridSpacing)
    assertEquals(3, layout.genreTileColumns)
    assertEquals(2, layout.savedMovieColumns)
  }

  @Test fun layoutSwitchesAtSixHundredDp() {
    assertEquals(CompactScreenLayout, layoutForWindow(599, 840))
    assertEquals(MediumScreenLayout, layoutForWindow(600, 840))
  }

  @Test fun widerThanMediumKeepsMediumLayout() {
    assertEquals(MediumScreenLayout, screenLayoutFor(WindowWidthSizeClass.Expanded))
    assertEquals(MediumScreenLayout, layoutForWindow(1280, 800))
  }
}
