package ee.movara.app.ui.home

import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.recommendation.MovieRecommendation
import ee.movara.app.ui.state.UiState
import org.junit.Assert.*
import org.junit.Test

class HomeUiStateTest {
  private val recommendation = MovieRecommendation(Movie(1, "Dune", null, 2021))

  @Test fun noRecommendationsProduceEmptyStateAfterLoading() {
    assertEquals(UiState.Empty, HomeUiState(loading = false).contentState)
  }

  @Test fun loadingDoesNotBecomeEmptyOrErrorBeforeItFinishes() {
    assertEquals(UiState.Loading, HomeUiState().contentState)
    assertEquals(UiState.Loading, HomeUiState(loadFailed = true).contentState)
  }

  @Test fun failedSourcesWithoutMoviesProduceError() {
    val state = HomeUiState(loading = false, loadFailed = true)
    assertTrue(state.contentState is UiState.Error)
  }

  @Test fun availableMoviesProduceSuccessEvenWithFailedSource() {
    val state = HomeUiState(
      loading = false,
      recommendations = listOf(recommendation),
      loadFailed = true
    )
    assertEquals(UiState.Success(listOf(recommendation)), state.contentState)
    assertEquals(recommendation.movie, state.topPick)
  }

  @Test fun availableMoviesWithoutFailuresProduceSuccess() {
    val state = HomeUiState(loading = false, recommendations = listOf(recommendation))
    assertEquals(UiState.Success(listOf(recommendation)), state.contentState)
  }
}
