
package ee.movara.app.ui.genres

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import ee.movara.app.ui.theme.MovaraTheme
import org.junit.Rule
import org.junit.Test

class GenresScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyGenresShowsMessageAndIcon() {
        compose.setContent {
            MovaraTheme {
                GenresScreen(
                    onGenreClick = {},
                    genres = emptyList()
                )
            }
        }

        compose.onNodeWithText(
            "Genres are unavailable right now."
        ).assertIsDisplayed()

        compose.onNodeWithTag(
            "empty_state_icon"
        ).assertIsDisplayed()
    }
}
