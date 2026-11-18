package ee.movara.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import ee.movara.app.ui.layout.ScreenLayoutForAvailableSize
import ee.movara.app.ui.theme.MovaraTheme
import org.junit.Rule
import org.junit.Test

/**
 * Checks which navigation component is visible in the two designed window sizes from
 * docs/design-specs.md section 6, and that resizing keeps the user on the same screen.
 */
class MainNavigationLayoutTest {

  @get:Rule
  val compose = createComposeRule()

  private val compactWindow = DpSize(412.dp, 917.dp)
  private val mediumWindow = DpSize(700.dp, 840.dp)

  private var windowSize by mutableStateOf(compactWindow)

  private val container: AppContainer
    get() = ApplicationProvider.getApplicationContext<MovaraApplication>().container

  // The real navigation, laid out as if the app window had the given size
  @Composable
  private fun NavigationInWindow() {
    DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(windowSize)) {
      MovaraTheme {
        ScreenLayoutForAvailableSize {
          AppNavigation(container)
        }
      }
    }
  }

  @Test
  fun compactWindowShowsBottomNavigationBar() {
    windowSize = compactWindow
    compose.setContent {
      NavigationInWindow()
    }

    compose.onNodeWithTag("main_bottom_bar").assertIsDisplayed()
    compose.onNodeWithTag("main_navigation_rail").assertDoesNotExist()
  }

  @Test
  fun mediumWindowShowsNavigationRail() {
    windowSize = mediumWindow
    compose.setContent {
      NavigationInWindow()
    }

    compose.onNodeWithTag("main_navigation_rail").assertIsDisplayed()
    compose.onNodeWithTag("main_bottom_bar").assertDoesNotExist()
  }

  @Test
  fun resizingKeepsTheCurrentDestination() {
    // MainActivity is recreated when its window is resized, so the test restores the
    // saved state after every size change, as the system does.
    val restorationTester = StateRestorationTester(compose)
    windowSize = compactWindow
    restorationTester.setContent {
      NavigationInWindow()
    }

    val genresTabInBottomBar = hasText("Genres") and hasAnyAncestor(hasTestTag("main_bottom_bar"))
    val genresTabInRail = hasText("Genres") and hasAnyAncestor(hasTestTag("main_navigation_rail"))

    compose.onNode(genresTabInBottomBar).performClick()
    // The Action tile exists only on the Genres screen
    compose.onNodeWithText("Action").assertIsDisplayed()

    // Compact to medium
    windowSize = mediumWindow
    restorationTester.emulateSavedInstanceStateRestore()

    compose.onNodeWithTag("main_navigation_rail").assertIsDisplayed()
    compose.onNodeWithTag("main_bottom_bar").assertDoesNotExist()
    compose.onNode(genresTabInRail).assertIsSelected()
    compose.onNodeWithText("Action").assertIsDisplayed()

    // Medium back to compact
    windowSize = compactWindow
    restorationTester.emulateSavedInstanceStateRestore()

    compose.onNodeWithTag("main_bottom_bar").assertIsDisplayed()
    compose.onNodeWithTag("main_navigation_rail").assertDoesNotExist()
    compose.onNode(genresTabInBottomBar).assertIsSelected()
    compose.onNodeWithText("Action").assertIsDisplayed()
  }
}
