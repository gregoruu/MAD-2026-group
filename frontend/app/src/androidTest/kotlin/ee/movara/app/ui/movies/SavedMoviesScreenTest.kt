package ee.movara.app.ui.movies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import ee.movara.app.data.local.MovieListType
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieLists
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
          SavedMoviesScreen(
            state = MovieListsUiState(MovieLists(liked = listOf(liked, later)), false),
            onMembershipChange = { _, _, _ -> }, onOpenMovie = {}, onAbout = {}, onRetry = {}
          )
        }
      }
    }
    val first = compose.onNodeWithText(liked.title).fetchSemanticsNode().boundsInRoot
    val second = compose.onNodeWithText(later.title).fetchSemanticsNode().boundsInRoot
    assertEquals(first.top, second.top, 1f)
    assertTrue(second.left > first.left)
  }

  @Test fun watchLaterActionsUpdateCountsAndMoveMovieToWatched() {
    val state = mutableStateOf(MovieListsUiState(MovieLists(watchLater = listOf(later)), false))
    var opened: Int? = null
    compose.setContent {
      MovaraTheme {
        SavedMoviesScreen(
          state = state.value,
          onMembershipChange = { movie, type, selected ->
            assertEquals(later, movie)
            assertEquals(MovieListType.WATCHED, type)
            assertTrue(selected)
            state.value = MovieListsUiState(MovieLists(watched = listOf(movie)), false)
          },
          onOpenMovie = { opened = it }, onAbout = {}, onRetry = {}
        )
      }
    }
    compose.onNode(hasText("Watch Later") and isSelectable()).performClick()
    compose.onNodeWithText("Mark Watched").performClick()
    compose.onNodeWithText("Your Watch Later list is empty.").assertIsDisplayed()
    compose.onNode(hasText("Watch Later") and isSelectable()).assert(hasText("0"))
    compose.onNode(hasText("Watched") and isSelectable()).assert(hasText("1")).performClick()
    compose.onNodeWithText(later.title).performClick()
    compose.runOnIdle { assertEquals(later.id, opened) }
  }

  @Test fun removeTargetsOnlyWatchLaterAndDoesNotOpenDetails() {
    var removal: Triple<Movie, MovieListType, Boolean>? = null
    var opened = false
    compose.setContent {
      MovaraTheme {
        SavedMoviesScreen(
          MovieListsUiState(MovieLists(watchLater = listOf(later)), false),
          onMembershipChange = { movie, type, selected -> removal = Triple(movie, type, selected) },
          onOpenMovie = { opened = true }, onAbout = {}, onRetry = {}
        )
      }
    }
    compose.onNode(hasText("Watch Later") and isSelectable()).performClick()
    compose.onNodeWithContentDescription("Remove Later movie from Watch Later").performClick()
    compose.runOnIdle {
      assertEquals(Triple(later, MovieListType.WATCH_LATER, false), removal)
      assertEquals(false, opened)
    }
  }

  @Test fun pendingWriteDisablesBothWatchLaterActions() {
    compose.setContent {
      MovaraTheme {
        SavedMoviesScreen(
          MovieListsUiState(MovieLists(watchLater = listOf(later)), false, pendingIds = setOf(later.id)),
          onMembershipChange = { _, _, _ -> error("Pending actions must be disabled") },
          onOpenMovie = {}, onAbout = {}, onRetry = {}
        )
      }
    }
    compose.onNode(hasText("Watch Later") and isSelectable()).performClick()
    compose.onNodeWithText("Mark Watched").assertIsNotEnabled()
    compose.onNodeWithContentDescription("Remove Later movie from Watch Later").assertIsNotEnabled()
  }

  @Test fun emptyMessagesAreSpecificToEachListAndAboutOpens() {
    var aboutOpened = false
    compose.setContent {
      MovaraTheme {
        SavedMoviesScreen(
          MovieListsUiState(isLoading = false),
          onMembershipChange = { _, _, _ -> }, onOpenMovie = {},
          onAbout = { aboutOpened = true }, onRetry = {}
        )
      }
    }
    compose.onNodeWithText("You have not liked any movies yet.").assertIsDisplayed()
    compose.onNode(hasText("Watch Later") and isSelectable()).performClick()
    compose.onNodeWithText("Your Watch Later list is empty.").assertIsDisplayed()
    compose.onNode(hasText("Watched") and isSelectable()).performClick()
    compose.onNodeWithText("You have not marked any movies as watched yet.").assertIsDisplayed()
    compose.onNodeWithContentDescription("About").performClick()
    compose.runOnIdle { assertTrue(aboutOpened) }
  }

  @Test fun loadingAndStorageErrorDoNotShowEmptyMessageOrStaleRows() {
    val state = mutableStateOf(MovieListsUiState(MovieLists(liked = listOf(liked))))
    var retried = false
    compose.setContent {
      MovaraTheme {
        SavedMoviesScreen(
          state.value, onMembershipChange = { _, _, _ -> }, onOpenMovie = {}, onAbout = {},
          onRetry = { retried = true }
        )
      }
    }
    compose.onNodeWithContentDescription("Loading your movies").assertIsDisplayed()
    compose.onNodeWithText(liked.title).assertDoesNotExist()
    compose.onNodeWithText("You have not liked any movies yet.").assertDoesNotExist()
    compose.runOnIdle { state.value = state.value.copy(isLoading = false, storageError = "Database unavailable") }
    compose.onNodeWithText(liked.title).assertDoesNotExist()
    compose.onNodeWithText("Could not read your saved movies. Please try again.").assertIsDisplayed()
    compose.onNodeWithText("Try again").performClick()
    compose.runOnIdle { assertTrue(retried) }
  }

  @Test fun selectedTabSurvivesStateRestoration() {
    val restoration = StateRestorationTester(compose)
    restoration.setContent {
      MovaraTheme {
        SavedMoviesScreen(
          MovieListsUiState(MovieLists(watched = listOf(watched)), false),
          onMembershipChange = { _, _, _ -> }, onOpenMovie = {}, onAbout = {}, onRetry = {}
        )
      }
    }
    compose.onNode(hasText("Watched") and isSelectable()).performClick()
    restoration.emulateSavedInstanceStateRestore()
    compose.onNode(hasText("Watched") and isSelectable()).assertIsSelected()
    compose.onNodeWithText(watched.title).assertIsDisplayed()
  }
}
