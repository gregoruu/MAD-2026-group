
package ee.movara.app.ui.state

import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.recommendation.MovieRecommendation
import ee.movara.app.ui.home.HomeUiState
import ee.movara.app.ui.search.SearchUiState
import ee.movara.app.ui.selectedgenre.SelectedGenreUiState
import org.junit.Assert.*
import org.junit.Test

class UiStateTest {

    private val movie = Movie(
        1, "Example", null, 2026
    )

    @Test
    fun sharedStateDistinguishesAllFourCases() {
        assertEquals(
            UiState.Loading,
            listUiState(emptyList<Movie>(), true)
        )

        assertEquals(
            UiState.Success(listOf(movie)),
            listUiState(listOf(movie), false)
        )

        assertEquals(
            UiState.Empty,
            listUiState(emptyList<Movie>(), false)
        )

        assertEquals(
            UiState.Error("Offline"),
            listUiState(emptyList<Movie>(), false, "Offline")
        )

        // Existing results remain visible during pagination.
        assertEquals(
            UiState.Success(listOf(movie)),
            listUiState(listOf(movie), true, "Offline")
        )
    }

    @Test
    fun homeUsesSharedState() {
        assertEquals(
            UiState.Loading,
            HomeUiState().contentState
        )

        assertEquals(
            UiState.Empty,
            HomeUiState(loading = false).contentState
        )

        assertTrue(
            HomeUiState(
                loading = false,
                loadFailed = true
            ).contentState is UiState.Error
        )

        assertTrue(
            HomeUiState(
                loading = false,
                recommendations = listOf(MovieRecommendation(movie))
            ).contentState is UiState.Success
        )
    }

    @Test
    fun searchAndGenreUseSharedState() {
        assertEquals(
            UiState.Empty,
            SearchUiState().contentState
        )

        assertEquals(
            UiState.Loading,
            SearchUiState(
                query = "Dune",
                isLoading = true
            ).contentState
        )

        assertEquals(
            UiState.Empty,
            SearchUiState(
                query = "Dune",
                page = 1
            ).contentState
        )

        assertEquals(
            UiState.Loading,
            SelectedGenreUiState().contentState
        )

        assertEquals(
            UiState.Empty,
            SelectedGenreUiState(
                isLoading = false
            ).contentState
        )
    }
}
