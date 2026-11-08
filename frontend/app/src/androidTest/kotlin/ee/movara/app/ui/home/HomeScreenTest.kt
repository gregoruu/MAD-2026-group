package ee.movara.app.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.theme.MovaraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
  @get:Rule val compose = createComposeRule()
  private val movie = Movie(42, "Arrival", null, 2016)

  @Test fun spotlightActionsAndSearchCallTheirDestinations() {
    var searched = false
    var openedId: Int? = null
    var likedId: Int? = null
    var savedId: Int? = null
    compose.setContent {
      MovaraTheme {
        HomeContent(
          HomeUiState(loading = false, rails = listOf(HomeRail(movies = listOf(movie, Movie(99, "Dune", null, 2021))))),
          onSearch = { searched = true },
          onMovieClick = { openedId = it },
          onLike = { likedId = it.id },
          onWatchLater = { savedId = it.id },
          onRetry = {}
        )
      }
    }
    compose.onNodeWithContentDescription("Search movies").performClick()
    compose.runOnIdle { assertEquals(true, searched) }
    compose.onNodeWithText("Like").performScrollTo().performClick()
    compose.runOnIdle { assertEquals(42, likedId) }
    compose.onNodeWithText("Watch Later").performScrollTo().performClick()
    compose.runOnIdle { assertEquals(42, savedId) }
    compose.onNodeWithText("See Details").performScrollTo().performClick()
    compose.runOnIdle { assertEquals(42, openedId) }
    compose.onNodeWithText("Trending this week").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Dune").performScrollTo().performClick()
    compose.runOnIdle { assertEquals(99, openedId) }
  }

  @Test fun likedSourceTitlesAndSelectedActionLabelsAreVisible() {
    compose.setContent {
      MovaraTheme {
        HomeContent(
          HomeUiState(
            loading = false,
            rails = listOf(HomeRail(Movie(1, "Interstellar", null, 2014), listOf(movie))),
            likedIds = setOf(42),
            watchLaterIds = setOf(42)
          ),
          {}, {}, {}, {}, {}
        )
      }
    }
    compose.onNodeWithText("Liked").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Saved").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Because you liked Interstellar").performScrollTo().assertIsDisplayed()
  }
}
