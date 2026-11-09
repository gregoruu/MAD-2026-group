package ee.movara.app.ui.preferences

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.theme.MovaraTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MoviePreferenceQuizScreenTest {
  @get:Rule val compose = createComposeRule()

  @Test fun genreLimitStillAllowsDeselectionAndNoPreference() {
    var changed: Int? = null
    var cleared = false
    compose.setContent {
      MovaraTheme {
        MoviePreferenceQuizContent(
          MoviePreferenceQuizState(
            loadingProfile = false, genreIds = setOf(28, 18, 878, 53, 27)
          ),
          actions = QuizActions(toggleGenre = { changed = it }, clearGenres = { cleared = true })
        )
      }
    }
    compose.onNodeWithText("Comedy").assertIsNotEnabled()
    compose.onNodeWithText("Action").performClick()
    compose.runOnIdle { assertEquals(28, changed) }
    compose.onNodeWithText("No preference").performClick()
    compose.runOnIdle { assertTrue(cleared) }
  }

  @Test fun moviesCanBeSelectedAndFavouriteChipsRemoved() {
    val movie = Movie(603, "The Matrix", null, 1999)
    var selected: Int? = null
    var removed: Int? = null
    compose.setContent {
      MovaraTheme {
        MoviePreferenceQuizContent(
          MoviePreferenceQuizState(
            loadingProfile = false, step = QuizStep.MOVIES,
            movies = listOf(movie), favouriteMovieIds = setOf(603),
            favouriteTitles = mapOf(603 to "The Matrix")
          ),
          actions = QuizActions(
            toggleMovie = { selected = it.id }, removeFavourite = { removed = it }
          )
        )
      }
    }
    compose.onNodeWithContentDescription("Remove The Matrix from favourites").performClick()
    compose.runOnIdle { assertEquals(603, removed) }
    compose.onNode(hasText("The Matrix") and isToggleable()).performClick()
    compose.runOnIdle { assertEquals(603, selected) }
  }

  @Test fun networkErrorOffersRetryAndDoesNotDisableContinue() {
    var retried = false
    var continued = false
    compose.setContent {
      MovaraTheme {
        MoviePreferenceQuizContent(
          MoviePreferenceQuizState(
            loadingProfile = false, step = QuizStep.MOVIES, movieLoadFailed = true
          ),
          actions = QuizActions(
            retryMovies = { retried = true }, next = { continued = true }
          )
        )
      }
    }
    compose.onNodeWithText("Try again").performClick()
    compose.onNodeWithText("Continue").assertIsEnabled().performClick()
    compose.runOnIdle {
      assertTrue(retried)
      assertTrue(continued)
    }
  }

  @Test fun ratingCanBeChangedAndEditedPreferencesSaved() {
    val state = mutableStateOf(MoviePreferenceQuizState(
      loadingProfile = false, step = QuizStep.RATING, minimumRating = 7
    ))
    var saved = false
    compose.setContent {
      MovaraTheme {
        MoviePreferenceQuizContent(
          state.value,
          isEditing = true,
          actions = QuizActions(
            ratingSelected = { state.value = state.value.copy(minimumRating = it) },
            next = { saved = true }
          )
        )
      }
    }
    compose.onNodeWithText("8+").performClick()
    compose.onNodeWithText("8+").assertIsSelected()
    compose.onNodeWithText("Save preferences").performClick()
    compose.runOnIdle { assertTrue(saved) }
  }

  @Test fun saveFailureAllowsFirstLaunchUserToContinueWithoutSaving() {
    var continued = false
    compose.setContent {
      MovaraTheme {
        MoviePreferenceQuizContent(
          MoviePreferenceQuizState(
            loadingProfile = false, step = QuizStep.RATING, saveFailed = true
          ),
          actions = QuizActions(continueWithoutSaving = { continued = true })
        )
      }
    }
    compose.onNodeWithText("Continue without saving").assertIsDisplayed().performClick()
    compose.runOnIdle { assertTrue(continued) }
  }
}
