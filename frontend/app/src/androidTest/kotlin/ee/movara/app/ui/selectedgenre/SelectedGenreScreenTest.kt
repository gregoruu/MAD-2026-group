
package ee.movara.app.ui.selectedgenre

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.theme.MovaraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag



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

    @Test
    fun initialLoadingShowsPosterShapedPlaceholders() {
        composeRule.setContent {
            MovaraTheme {
                SelectedGenreScreen(
                    genreId = 28,
                    movies = emptyList(),
                    isLoading = true,
                    onBack = {},
                    onMovieClick = {}
                )
            }
        }

        assertTrue(
            composeRule
                .onAllNodesWithTag("genre_loading_placeholder")
                .fetchSemanticsNodes()
                .isNotEmpty()
        )

        val bounds = composeRule
            .onAllNodesWithTag("genre_loading_placeholder")
            .onFirst()
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(
            144f / 208f,
            bounds.width / bounds.height,
            0.05f
        )

    }


    @Test
    fun paginationLoadingKeepsMoviesAndAnnouncesLoading() {
        composeRule.setContent {
            MovaraTheme {
                SelectedGenreScreen(
                    genreId = 28,
                    movies = listOf(
                        Movie(1, "Example", null, 2026)
                    ),
                    isLoadingMore = true,
                    onBack = {},
                    onMovieClick = {}
                )
            }
        }

        // Existing movie remains visible.
        composeRule.onNodeWithText("Example")
            .assertIsDisplayed()

        // Loading placeholders are present.
        assertTrue(
            composeRule
                .onAllNodesWithTag(
                    "genre_loading_more_placeholder"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        )

        // One accessible loading announcement.
        composeRule.onNodeWithTag("genre_loading_more_status")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite
                )
            )

        // The old indeterminate spinner is no longer present.
        composeRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(
                SemanticsProperties.ProgressBarRangeInfo
            )
        ).assertCountEquals(0)
    }

}
