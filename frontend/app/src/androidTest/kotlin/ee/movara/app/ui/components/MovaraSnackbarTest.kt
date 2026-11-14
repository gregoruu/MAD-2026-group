
package ee.movara.app.ui.components

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ee.movara.app.ui.theme.MovaraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MovaraSnackbarTest {

    @get:Rule
    val compose = createComposeRule()

    private class FakeSnackbarData(
        text: String,
        label: String? = null
    ) : SnackbarData {

        var actionCalls = 0
        var dismissCalls = 0

        override val visuals: SnackbarVisuals = object : SnackbarVisuals {
            override val message = text
            override val actionLabel = label
            override val withDismissAction = true
            override val duration = SnackbarDuration.Short
        }

        override fun performAction() {
            actionCalls++
        }

        override fun dismiss() {
            dismissCalls++
        }
    }

    @Test
    fun addingMovieShowsConfirmationWithoutUndo() {
        val data = FakeSnackbarData(
            text = "Added to Watch Later"
        )

        compose.setContent {
            MovaraTheme {
                MovaraSnackbar(data)
            }
        }

        compose.onNodeWithText("Added to Watch Later")
            .assertIsDisplayed()

        compose.onNodeWithText("Undo")
            .assertDoesNotExist()
    }

    @Test
    fun removingMovieShowsUndoAndActionWorks() {
        val data = FakeSnackbarData(
            text = "Removed from Watch Later",
            label = "Undo"
        )

        compose.setContent {
            MovaraTheme {
                MovaraSnackbar(data)
            }
        }

        compose.onNodeWithText("Removed from Watch Later")
            .assertIsDisplayed()

        compose.onNodeWithText("Undo")
            .assertIsDisplayed()
            .performClick()

        compose.runOnIdle {
            assertEquals(1, data.actionCalls)
        }
    }

    @Test
    fun failedUpdateShowsRetryAction() {
        val data = FakeSnackbarData(
            text = "Could not update Liked: Storage unavailable",
            label = "Retry"
        )

        compose.setContent {
            MovaraTheme {
                MovaraSnackbar(data)
            }
        }

        compose.onNodeWithText(
            "Could not update Liked: Storage unavailable"
        ).assertIsDisplayed()

        compose.onNodeWithText("Retry")
            .performClick()

        compose.runOnIdle {
            assertEquals(1, data.actionCalls)
        }
    }
}
