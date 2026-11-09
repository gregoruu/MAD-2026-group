package ee.movara.app.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.recommendation.MovieRecommendation
import ee.movara.app.domain.recommendation.RecommendationSource
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
          HomeUiState(loading = false, recommendations = listOf(
            MovieRecommendation(movie),
            MovieRecommendation(Movie(99, "Dune", null, 2021))
          )),
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
    compose.onNodeWithText("More to discover").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Dune").performScrollTo().performClick()
    compose.runOnIdle { assertEquals(99, openedId) }
  }

  @Test fun favouriteReasonsAndSelectedActionLabelsAreVisible() {
    compose.setContent {
      MovaraTheme {
        HomeContent(
          HomeUiState(
            loading = false,
            recommendations = listOf(MovieRecommendation(
              movie, setOf(RecommendationSource.Seed(1, "Interstellar"))
            )),
            likedIds = setOf(42),
            watchLaterIds = setOf(42)
          ),
          {}, {}, {}, {}, {}
        )
      }
    }
    compose.onNodeWithText("Because you enjoyed Interstellar").assertIsDisplayed()
    compose.onNodeWithText("Liked").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Saved").performScrollTo().assertIsDisplayed()
  }

  @Test fun genreBasedRecommendationsExplainTheMatch() {
    compose.setContent {
      MovaraTheme {
        HomeContent(
          HomeUiState(loading = false, recommendations = listOf(
            MovieRecommendation(movie, preferredGenreIds = setOf(878))
          )),
          {}, {}, {}, {}, {}
        )
      }
    }
    compose.onNodeWithText("Matches your interest in Sci-Fi").assertIsDisplayed()
  }

  @Test fun cachedPartialResultsRemainUsableAndOfferRetry() {
    var retries = 0
    compose.setContent {
      MovaraTheme {
        HomeContent(
          HomeUiState(loading = false, loadFailed = true, usingCache = true,
            recommendations = listOf(MovieRecommendation(movie))),
          {}, {}, {}, {}, { retries++ }
        )
      }
    }
    compose.onNodeWithText("Showing saved recommendations. They may be out of date.")
      .assertIsDisplayed()
    compose.onNodeWithText("Some recommendations couldn’t load. Showing the available movies.")
      .performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Try again").performScrollTo().performClick()
    compose.runOnIdle { assertEquals(1, retries) }
    compose.onNodeWithText("Arrival").performScrollTo().assertIsDisplayed()
  }

  @Test fun ratingFilteredEmptyStateExplainsTheMinimumAndAllowsEditingPreferences() {
    var edited = false
    compose.setContent {
      MovaraTheme {
        HomeContent(
          HomeUiState(loading = false, minimumRating = 8),
          {}, {}, {}, {}, {},
          onEditPreferences = { edited = true }
        )
      }
    }
    compose.onNodeWithText(
      "No recommendations meet your 8+ minimum right now. Try again or edit Your taste."
    ).assertIsDisplayed()
    compose.onNodeWithText("Your taste").performClick()
    compose.runOnIdle { assertEquals(true, edited) }
  }
}
