package ee.movara.app.ui.preferences

import ee.movara.app.domain.model.MovieTasteProfile
import ee.movara.app.domain.model.OnboardingStatus
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenceStartupViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val repository = QuizTasteRepository()

  @Before fun setUp() { Dispatchers.setMain(dispatcher) }
  @After fun tearDown() { Dispatchers.resetMain() }

  @Test fun firstLaunchWaitsForProfileThenShowsQuiz() = runTest(dispatcher) {
    val model = PreferenceStartupViewModel(repository)
    assertEquals(PreferenceStartupState.LOADING, model.uiState.value)
    advanceUntilIdle()
    assertEquals(PreferenceStartupState.QUIZ, model.uiState.value)
  }

  @Test fun completedAndSkippedProfilesGoDirectlyToHome() = runTest(dispatcher) {
    for (status in listOf(OnboardingStatus.COMPLETED, OnboardingStatus.SKIPPED)) {
      repository.stored.value = MovieTasteProfile(onboardingStatus = status)
      val model = PreferenceStartupViewModel(repository)
      advanceUntilIdle()
      assertEquals(PreferenceStartupState.READY, model.uiState.value)
    }
  }

  @Test fun readFailureCanBeRetriedWithoutResettingProfile() = runTest(dispatcher) {
    repository.stored.value = MovieTasteProfile(onboardingStatus = OnboardingStatus.COMPLETED)
    repository.readFailure = IOException()
    val model = PreferenceStartupViewModel(repository)
    advanceUntilIdle()
    assertEquals(PreferenceStartupState.ERROR, model.uiState.value)
    repository.readFailure = null
    model.retry()
    advanceUntilIdle()
    assertEquals(PreferenceStartupState.READY, model.uiState.value)
    assertEquals(OnboardingStatus.COMPLETED, repository.stored.value.onboardingStatus)
  }

  @Test fun continueAfterStorageFailureOnlyDismissesQuizForCurrentSession() = runTest(dispatcher) {
    repository.readFailure = IOException()
    val model = PreferenceStartupViewModel(repository)
    advanceUntilIdle()
    model.enterApp()
    assertEquals(PreferenceStartupState.READY, model.uiState.value)
    assertTrue(repository.stored.value.needsOnboarding)
    assertEquals(0, repository.saveCalls)
    assertEquals(0, repository.skipCalls)
  }
}
