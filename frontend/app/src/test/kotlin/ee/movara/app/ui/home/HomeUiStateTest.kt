
package ee.movara.app.ui.home

import ee.movara.app.domain.model.Movie
import ee.movara.app.ui.state.UiState
import org.junit.Assert.*
import org.junit.Test

class HomeUiStateTest {

    @Test
    fun emptyRailsProduceEmptyState() {
        assertEquals(
            UiState.Empty,
            HomeUiState(
                loading = false,
                rails = emptyList()
            ).contentState
        )

        assertEquals(
            UiState.Empty,
            HomeUiState(
                loading = false,
                rails = listOf(
                    HomeRail(movies = emptyList())
                )
            ).contentState
        )
    }

    @Test
    fun failedRailsWithoutMoviesProduceError() {
        val state = HomeUiState(
            loading = false,
            rails = listOf(
                HomeRail(failed = true),
                HomeRail(failed = true)
            )
        )

        assertTrue(state.contentState is UiState.Error)
    }

    @Test
    fun availableMoviesProduceSuccessEvenWithFailedRail() {
        val state = HomeUiState(
            loading = false,
            rails = listOf(
                HomeRail(
                    movies = listOf(
                        Movie(1, "Dune", null, 2021)
                    )
                ),
                HomeRail(failed = true)
            )
        )

        assertTrue(state.contentState is UiState.Success)
    }
}
