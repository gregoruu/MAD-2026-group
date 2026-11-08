package ee.movara.app.ui.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.theme.MovaraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
  @get:Rule val compose = createComposeRule()

  @Test fun emptyFieldShowsPlaceholderAndSupportsSearchSubmission() {
    var submissions = 0
    compose.setContent {
      MovaraTheme {
        SearchScreen(
          state = SearchUiState(),
          onQueryChange = {},
          onSearch = { submissions++ },
          onLoadMore = {},
          onRetry = {},
          onMovieClick = {},
          onBack = {}
        )
      }
    }
    compose.onNodeWithText("Search movies by title").assertIsDisplayed()
    compose.onNode(hasSetTextAction()).performImeAction()
    compose.runOnIdle { assertEquals(1, submissions) }
  }

  @Test fun emptySubmissionValidationIsVisible() {
    compose.setContent {
      MovaraTheme {
        SearchScreen(
          state = SearchUiState(showTitleError = true),
          onQueryChange = {},
          onSearch = {},
          onLoadMore = {},
          onRetry = {},
          onMovieClick = {},
          onBack = {}
        )
      }
    }
    compose.onNodeWithText("Enter a movie title").assertIsDisplayed()
  }

  @Test fun clearButtonSendsEmptyQuery() {
    var query: String? = null
    compose.setContent {
      MovaraTheme {
        SearchScreen(
          state = SearchUiState(query = "Alien"),
          onQueryChange = { query = it },
          onSearch = {},
          onLoadMore = {},
          onRetry = {},
          onMovieClick = {},
          onBack = {}
        )
      }
    }
    compose.onNodeWithContentDescription("Clear search").performClick()
    compose.runOnIdle { assertEquals("", query) }
  }

  @Test fun gridShowsThreeColumnsWithTitlesYearsAndMovieClick() {
    val movies = (1..4).map { Movie(it, "Movie $it", null, 2020 + it) }
    var selectedId: Int? = null
    compose.setContent {
      MovaraTheme {
        SearchScreen(
          state = SearchUiState(query = "Movie", movies = movies, page = 1, totalPages = 1),
          onQueryChange = {},
          onSearch = {},
          onLoadMore = {},
          onRetry = {},
          onMovieClick = { selectedId = it },
          onBack = {}
        )
      }
    }
    val first = compose.onNodeWithText("Movie 1").fetchSemanticsNode().boundsInRoot
    val second = compose.onNodeWithText("Movie 2").fetchSemanticsNode().boundsInRoot
    val third = compose.onNodeWithText("Movie 3").fetchSemanticsNode().boundsInRoot
    val fourth = compose.onNodeWithText("Movie 4").fetchSemanticsNode().boundsInRoot
    assertEquals(first.top, second.top, 1f)
    assertEquals(second.top, third.top, 1f)
    assertTrue(first.left < second.left && second.left < third.left)
    assertTrue(fourth.top > first.top)
    compose.onNodeWithText("2021").assertIsDisplayed()
    compose.onNodeWithText("Movie 2").performClick()
    compose.runOnIdle { assertEquals(2, selectedId) }
  }
}
