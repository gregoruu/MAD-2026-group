package ee.movara.app.ui.preferences

import androidx.lifecycle.SavedStateHandle
import ee.movara.app.data.repository.ErrorCategory
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.MovieTastePreferences
import ee.movara.app.domain.model.MovieDiscovery
import java.time.LocalDate
import ee.movara.app.domain.model.MovieTasteProfile
import ee.movara.app.domain.model.OnboardingStatus
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoviePreferenceQuizViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val taste = QuizTasteRepository()
  private val movies = QuizMovieRepository()
  private val saved = SavedStateHandle()

  @Before fun setUp() { Dispatchers.setMain(dispatcher) }
  @After fun tearDown() { Dispatchers.resetMain() }

  private val today = LocalDate.of(2026, 11, 9)
  private fun model() = MoviePreferenceQuizViewModel(taste, movies, saved) { today }

  @Test fun draftDoesNotChangeStoredPreferencesOrCompleteOnboarding() = runTest(dispatcher) {
    val model = model()
    advanceUntilIdle()
    model.toggleGenre(28)
    model.toggleMovie(quizMovie(603))
    model.selectRating(7)
    assertEquals(MovieTasteProfile(), taste.stored.value)
    assertEquals(0, taste.saveCalls)
    assertEquals(0, taste.skipCalls)
  }

  @Test fun atMostFiveGenresCanBeSelectedAndNoPreferenceClearsThem() = runTest(dispatcher) {
    val model = model()
    advanceUntilIdle()
    listOf(28, 18, 878, 53, 27, 35).forEach(model::toggleGenre)
    assertEquals(setOf(28, 18, 878, 53, 27), model.uiState.value.genreIds)
    model.toggleGenre(28)
    model.toggleGenre(35)
    assertTrue(35 in model.uiState.value.genreIds)
    model.clearGenres()
    assertTrue(model.uiState.value.genreIds.isEmpty())
  }

  @Test fun genreResultsAreInterleavedAndDuplicatesRemoved() = runTest(dispatcher) {
    movies.respond = { source, page ->
      quizPage(if (source == "discover:28") listOf(quizMovie(1), quizMovie(2))
        else listOf(quizMovie(3), quizMovie(2)), page)
    }
    val model = model()
    advanceUntilIdle()
    model.toggleGenre(28)
    model.toggleGenre(35)
    model.next()
    advanceUntilIdle()
    assertEquals(setOf("discover:28" to 1, "discover:35" to 1), movies.requests.toSet())
    assertEquals(listOf(1, 3, 2), model.uiState.value.movies.map { it.id })
  }

  @Test fun noGenresUsesFamiliarDiscoveryAndCanContinueWithoutMovieSelections() = runTest(dispatcher) {
    val model = model()
    advanceUntilIdle()
    model.next()
    advanceUntilIdle()
    assertEquals(listOf("discover:all" to 1), movies.requests)
    model.next()
    model.finish()
    advanceUntilIdle()
    assertEquals(MovieTastePreferences(), taste.stored.value.preferences)
    assertFalse(taste.stored.value.needsOnboarding)
  }

  @Test fun searchDebouncesAndKeepsExistingSelections() = runTest(dispatcher) {
    val model = model()
    advanceUntilIdle()
    model.next()
    advanceUntilIdle()
    model.toggleMovie(quizMovie(603, "The Matrix"))
    movies.requests.clear()
    model.onQueryChange("Bat")
    advanceTimeBy(300)
    model.onQueryChange("Batman")
    advanceTimeBy(399)
    runCurrent()
    assertTrue(movies.requests.isEmpty())
    advanceTimeBy(1)
    advanceUntilIdle()
    assertEquals(listOf("search:Batman" to 1), movies.requests)
    assertEquals(setOf(603), model.uiState.value.favouriteMovieIds)
    model.onQueryChange("")
    advanceUntilIdle()
    assertEquals("discover:all" to 1, movies.requests.last())
    assertEquals(setOf(603), model.uiState.value.favouriteMovieIds)
  }

  @Test fun cancelledSearchCannotReplaceTheNewQueryResults() = runTest(dispatcher) {
    val model = model()
    advanceUntilIdle()
    model.next()
    advanceUntilIdle()
    movies.respond = { source, page ->
      withContext(NonCancellable) { delay(if (source == "search:Old") 2000 else 10) }
      quizPage(listOf(quizMovie(1, source)), page)
    }
    model.onQueryChange("Old")
    advanceTimeBy(400)
    runCurrent()
    model.onQueryChange("New")
    advanceUntilIdle()
    assertEquals("search:New", model.uiState.value.movies.single().title)
    assertFalse(model.uiState.value.loadingMovies)
  }

  @Test fun failedNextPageKeepsMoviesAndRetriesTheSamePage() = runTest(dispatcher) {
    val model = model()
    advanceUntilIdle()
    model.next()
    advanceUntilIdle()
    movies.respond = { _, _ -> RepositoryResult.Error(ErrorCategory.NETWORK, "Offline") }
    model.loadMore()
    advanceUntilIdle()
    assertEquals(listOf(1), model.uiState.value.movies.map { it.id })
    assertTrue(model.uiState.value.movieLoadFailed)
    movies.respond = { _, page -> quizPage(listOf(quizMovie(2)), page) }
    model.retryMovies()
    advanceUntilIdle()
    assertEquals(listOf("discover:all" to 1, "discover:all" to 2, "discover:all" to 2), movies.requests)
    assertEquals(listOf(1, 2), model.uiState.value.movies.map { it.id })
    assertFalse(model.uiState.value.canLoadMore)
  }

  @Test fun exhaustedGenreIsNotRequestedBeyondItsLastPage() = runTest(dispatcher) {
    movies.respond = { source, page ->
      quizPage(listOf(quizMovie(page)), page, if (source == "discover:28") 1 else 2)
    }
    val model = model()
    advanceUntilIdle()
    model.toggleGenre(28)
    model.toggleGenre(35)
    model.next()
    advanceUntilIdle()
    model.loadMore()
    advanceUntilIdle()
    assertFalse("discover:28" to 2 in movies.requests)
    assertTrue("discover:35" to 2 in movies.requests)
  }

  @Test fun networkFailureDoesNotBlockNextStepOrFinishing() = runTest(dispatcher) {
    movies.respond = { _, _ -> RepositoryResult.Error(ErrorCategory.NETWORK, "Offline") }
    val model = model()
    advanceUntilIdle()
    model.toggleGenre(28)
    model.next()
    advanceUntilIdle()
    assertTrue(model.uiState.value.movieLoadFailed)
    model.next()
    model.selectRating(8)
    model.finish()
    advanceUntilIdle()
    assertTrue(model.uiState.value.finished)
    assertEquals(MovieTastePreferences(genreIds = setOf(28), minimumRating = 8),
      taste.stored.value.preferences)
  }

  @Test fun skipDiscardsDraftInsteadOfSavingIt() = runTest(dispatcher) {
    val model = model()
    advanceUntilIdle()
    model.toggleGenre(28)
    model.selectRating(7)
    model.skip()
    advanceUntilIdle()
    assertEquals(MovieTasteProfile(onboardingStatus = OnboardingStatus.SKIPPED), taste.stored.value)
    assertEquals(0, taste.saveCalls)
    assertTrue(model.uiState.value.finished)
  }

  @Test fun saveFailureKeepsDraftAndRetryCanFinish() = runTest(dispatcher) {
    taste.writeFailure = IOException()
    val model = model()
    advanceUntilIdle()
    model.toggleGenre(28)
    model.toggleMovie(quizMovie(603))
    model.selectRating(7)
    model.finish()
    advanceUntilIdle()
    assertTrue(model.uiState.value.saveFailed)
    assertFalse(model.uiState.value.finished)
    assertEquals(MovieTasteProfile(), taste.stored.value)
    taste.writeFailure = null
    model.finish()
    advanceUntilIdle()
    assertTrue(model.uiState.value.finished)
    assertEquals(MovieTastePreferences(setOf(28), setOf(603), 7), taste.stored.value.preferences)
  }

  @Test fun saveCompletesOnlyAfterSuccessfulWriteAndIgnoresDoubleTap() = runTest(dispatcher) {
    taste.saveDelay = 1000
    val model = model()
    advanceUntilIdle()
    model.finish()
    model.finish()
    runCurrent()
    assertTrue(model.uiState.value.saving)
    assertFalse(model.uiState.value.finished)
    assertEquals(1, taste.saveCalls)
    advanceUntilIdle()
    assertTrue(model.uiState.value.finished)
  }

  @Test fun existingPreferencesCanBeEditedWithoutChangingThemUntilSave() = runTest(dispatcher) {
    val initial = MovieTastePreferences(setOf(28), setOf(603), 7)
    taste.stored.value = MovieTasteProfile(initial, OnboardingStatus.COMPLETED)
    val model = model()
    advanceUntilIdle()
    assertEquals(initial.genreIds, model.uiState.value.genreIds)
    assertEquals(initial.favouriteMovieIds, model.uiState.value.favouriteMovieIds)
    model.clearGenres()
    model.clearFavourites()
    model.selectRating(null)
    assertEquals(initial, taste.stored.value.preferences)
    model.finish()
    advanceUntilIdle()
    assertEquals(MovieTastePreferences(), taste.stored.value.preferences)
  }

  @Test fun restoredDraftPreservesSelectionsStepAndQueryWithoutPersistingThem() = runTest(dispatcher) {
    val model = model()
    advanceUntilIdle()
    model.toggleGenre(28)
    model.toggleMovie(quizMovie(603, "The Matrix"))
    model.next()
    model.onQueryChange("Alien")
    model.next()
    model.selectRating(6)
    advanceUntilIdle()
    val restoredHandle = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
    val restored = MoviePreferenceQuizViewModel(taste, movies, restoredHandle)
    advanceUntilIdle()
    assertEquals(QuizStep.RATING, restored.uiState.value.step)
    assertEquals(setOf(28), restored.uiState.value.genreIds)
    assertEquals(setOf(603), restored.uiState.value.favouriteMovieIds)
    assertEquals("The Matrix", restored.uiState.value.favouriteTitles[603])
    assertEquals("Alien", restored.uiState.value.query)
    assertEquals(6, restored.uiState.value.minimumRating)
    assertEquals(MovieTasteProfile(), taste.stored.value)
  }

  @Test fun discoveryUsesFamiliarFiltersAndAFixedReleaseDateForEveryPage() = runTest(dispatcher) {
    val model = model()
    advanceUntilIdle()
    model.toggleGenre(28)
    model.next()
    advanceUntilIdle()
    model.loadMore()
    advanceUntilIdle()
    assertEquals(listOf(
      MovieDiscovery.familiar(28, today) to 1,
      MovieDiscovery.familiar(28, today) to 2
    ), movies.discoveryRequests)
  }

  @Test fun browsingExcludesUnreleasedUnknownAndInsufficientlyRatedFilms() = runTest(dispatcher) {
    movies.respond = { _, page ->
      quizPage(listOf(
        quizMovie(1),
        quizMovie(2).copy(releaseDate = today.plusDays(1)),
        quizMovie(3).copy(releaseDate = null),
        quizMovie(4).copy(rating = 5.9),
        quizMovie(5).copy(voteCount = 99),
        quizMovie(6).copy(rating = null),
        quizMovie(7).copy(releaseDate = today, rating = 6.0, voteCount = 100)
      ), page)
    }
    val model = model()
    advanceUntilIdle()
    model.next()
    advanceUntilIdle()
    assertEquals(listOf(1, 7), model.uiState.value.movies.map { it.id })
  }

  @Test fun titleSearchAllowsLowRatedFavouritesButExcludesUnreleasedFilms() = runTest(dispatcher) {
    val model = model()
    advanceUntilIdle()
    model.next()
    advanceUntilIdle()
    movies.respond = { _, page ->
      quizPage(listOf(
        quizMovie(1).copy(rating = 4.0, voteCount = 10),
        quizMovie(2).copy(releaseDate = today.plusDays(1))
      ), page)
    }
    model.onQueryChange("Favourite")
    advanceUntilIdle()
    assertEquals(listOf(1), model.uiState.value.movies.map { it.id })
  }

  @Test fun profileReadFailureDoesNotSaveDefaultsAndCanBeRetried() = runTest(dispatcher) {
    taste.readFailure = IOException()
    val model = model()
    advanceUntilIdle()
    assertTrue(model.uiState.value.profileLoadFailed)
    model.finish()
    assertEquals(0, taste.saveCalls)
    taste.readFailure = null
    model.retryProfile()
    advanceUntilIdle()
    assertFalse(model.uiState.value.profileLoadFailed)
    assertFalse(model.uiState.value.loadingProfile)
  }
}
