package ee.movara.app.ui.movies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieLists
import ee.movara.app.ui.layout.ScreenLayoutForAvailableSize
import ee.movara.app.ui.theme.MovaraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SavedMoviesScreenTest {
  @get:Rule val compose = createComposeRule()

  private val liked = Movie(1, "Liked movie", null, 2025)
  private val later = Movie(2, "Later movie", null, 2024)
  private val watched = Movie(3, "Watched movie", null, 2023)

  @Test fun tabsShowCountsAndOnlySelectedMovies() {
    compose.setContent {
      MovaraTheme {
        SavedMoviesScreen(
          state = MovieListsUiState(MovieLists(listOf(liked), listOf(later), listOf(watched)), false),
          onMembershipChange = { _, _, _ -> }, onOpenMovie = {}, onAbout = {}, onRetry = {}
        )
      }
    }
    compose.onAllNodesWithText("1").assertCountEquals(3)
    compose.onNodeWithText(liked.title).assertIsDisplayed()
    compose.onNodeWithText(later.title).assertDoesNotExist()
    compose.onNode(hasText("Watch Later") and isSelectable()).performClick()
    compose.onNodeWithText(later.title).assertIsDisplayed()
    compose.onNodeWithText(liked.title).assertDoesNotExist()
    compose.onNode(hasText("Watched") and isSelectable()).performClick()
    compose.onNodeWithText(watched.title).assertIsDisplayed()
    compose.onNodeWithText(later.title).assertDoesNotExist()
  }

  @Test fun mediumWidthPlacesMoviesInTwoColumns() {
    compose.setContent {
      MovaraTheme {
        Box(Modifier.requiredSize(700.dp, 840.dp)) {
          ScreenLayoutForAvailableSize {
            SavedMoviesScreen(
              state = MovieListsUiState(MovieLists(liked = listOf(liked, later)), false),
              onMembershipChange = { _, _, _ -> }, onOpenMovie = {}, onAbout = {}, onRetry = {}
            )
          }
        }
      }
    }
    val first = compose.onNodeWithText(liked.title).fetchSemanticsNode().boundsInRoot
    val second = compose.onNodeWithText(later.title).fetchSemanticsNode().boundsInRoot
    assertEquals(first.top, second.top, 1f)
    assertTrue(second.left > first.left)
  }
}
