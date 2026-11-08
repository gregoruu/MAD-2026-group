
package ee.movara.app.ui.selectedgenre

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.theme.MovaraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SelectedGenreScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsGenreAndMovies() {
        val movies = listOf(
            Movie(1, "Test Action Movie", null, 2025),
            Movie(2, "Second Movie", null, 2024)
        )

        composeRule.setContent {
            MovaraTheme {
                SelectedGenreScreen(
                    genreId = 28,
                    movies = movies,
                    onBack = {},
                    onMovieClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Action")
            .assertExists()

        composeRule.onNodeWithText("Test Action Movie")
            .assertExists()

        composeRule.onNodeWithText("Second Movie")
            .assertExists()

        composeRule.onNodeWithText("All Genres")
            .assertExists()
    }

    @Test
    fun errorStateRetryButtonWorks() {
        var retryCount = 0

        composeRule.setContent {
            MovaraTheme {
                SelectedGenreScreen(
                    genreId = 28,
                    movies = emptyList(),
                    errorMessage = "Could not load movies.",
                    onRetry = { retryCount++ },
                    onBack = {},
                    onMovieClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Could not load movies.")
            .assertExists()

        composeRule.onNodeWithText("Try again")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, retryCount)
        }
    }
}
