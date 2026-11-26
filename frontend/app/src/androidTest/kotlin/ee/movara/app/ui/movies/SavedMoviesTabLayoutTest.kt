package ee.movara.app.ui.movies

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieLists
import ee.movara.app.ui.layout.ScreenLayoutForAvailableSize
import ee.movara.app.ui.theme.MovaraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SavedMoviesTabLayoutTest(
  private val width: Int,
  private val fontScale: Float,
  private val hasMovies: Boolean
) {
  @get:Rule val compose = createComposeRule()

  @Test fun tabsKeepLabelsAndCountsAlignedAndSwitchLists() {
    val labels = listOf("Liked", "Watch Later", "Watched")
    val titles = listOf("Liked movie", "Later movie", "Watched movie")
    val emptyMessages = listOf(
      "You have not liked any movies yet.",
      "Your Watch Later list is empty.",
      "You have not marked any movies as watched yet."
    )
    val movies = titles.mapIndexed { index, title -> Movie(index + 1, title, null, 2025) }
    val lists = if (hasMovies) {
      MovieLists(listOf(movies[0]), listOf(movies[1]), listOf(movies[2]))
    } else MovieLists()
    val count = if (hasMovies) "1" else "0"

    compose.setContent {
      DeviceConfigurationOverride(
        DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, 840.dp)) then
          DeviceConfigurationOverride.FontScale(fontScale)
      ) {
        MovaraTheme {
          ScreenLayoutForAvailableSize {
            SavedMoviesScreen(
              MovieListsUiState(lists, false),
              onMembershipChange = { _, _, _ -> }, onOpenMovie = {}, onAbout = {}, onRetry = {}
            )
          }
        }
      }
    }

    labels.forEachIndexed { selectedIndex, label ->
      val selectedTab = compose.onNode(hasText(label) and isSelectable())
      selectedTab.performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
      labels.filter { it != label }.forEach { other ->
        compose.onNode(hasText(other) and isSelectable()).assertIsNotSelected()
      }
      compose.onNodeWithText(
        if (hasMovies) titles[selectedIndex] else emptyMessages[selectedIndex]
      ).assertIsDisplayed()

      val tabs = labels.map {
        compose.onNode(hasText(it) and isSelectable()).getUnclippedBoundsInRoot()
      }
      tabs.forEach {
        assertEquals(tabs.first().top.value, it.top.value, 1f)
        assertEquals(tabs.first().bottom.value, it.bottom.value, 1f)
        assertEquals(
          (tabs.first().right - tabs.first().left).value, (it.right - it.left).value, 1f
        )
        assertTrue(it.bottom - it.top >= 48.dp)
      }

      labels.forEach {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(it, useUnmergedTree = true)
          .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        val textLayout = results.single()
        assertEquals("Label must stay on one complete line: $it", 1, textLayout.lineCount)
        assertEquals(it.length, textLayout.getLineEnd(0))
        // Text layout rounds its pixel size; allow only that rounding difference.
        assertTrue(
          "Label must fit: $it; size=${textLayout.size}, right=${textLayout.getLineRight(0)}",
          textLayout.getLineRight(0) <= textLayout.size.width + 1f
        )
        assertTrue(textLayout.getLineBottom(0) <= textLayout.size.height + 1f)
      }
      compose.onAllNodesWithText(count, useUnmergedTree = true).assertCountEquals(3)
      val counts = labels.map { tabLabel ->
        compose.onNode(
          hasText(count) and hasAnyAncestor(
            isSelectable() and hasAnyDescendant(hasText(tabLabel))
          ),
          useUnmergedTree = true
        )
      }
      counts[selectedIndex].assertIsDisplayed()
      repeat(3) { index ->
        val bounds = counts[index].getUnclippedBoundsInRoot()
        assertEquals(counts[0].getUnclippedBoundsInRoot().top.value, bounds.top.value, 1f)
        assertTrue(bounds.bottom <= tabs[index].bottom)
        val labelBounds = compose.onNodeWithText(labels[index], useUnmergedTree = true)
          .getUnclippedBoundsInRoot()
        assertTrue(bounds.top >= labelBounds.bottom)
        assertEquals(
          (tabs[index].left + tabs[index].right).value / 2,
          (bounds.left + bounds.right).value / 2,
          1f
        )
      }
    }
    // At the design's default compact/medium sizes, all tabs fit without scrolling.
    if (fontScale == 1f && width >= 412) {
      labels.forEach {
        compose.onNode(hasText(it) and isSelectable()).assertIsDisplayed()
      }
    }
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}dp_font{1}_saved{2}")
    fun configurations(): List<Array<Any>> =
      listOf(320, 412, 700).flatMap { width ->
        listOf(1f, 1.5f, 2f).flatMap { scale ->
          listOf(false, true).map { saved -> arrayOf<Any>(width, scale, saved) }
        }
      }
  }
}
