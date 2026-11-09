package ee.movara.app.ui.search

import androidx.lifecycle.SavedStateHandle
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.DataSource
import ee.movara.app.data.repository.ErrorCategory
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieDetails
import ee.movara.app.domain.model.MoviePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
import ee.movara.app.ui.state.UiState

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val repository = SearchRepository()
  private val savedState = SavedStateHandle()

  @Before fun setUp() { Dispatchers.setMain(dispatcher) }
  @After fun tearDown() { Dispatchers.resetMain() }

  @Test fun typingWaits400msAndOnlyRequestsLatestTitle() = runTest(dispatcher) {
    val model = SearchViewModel(repository, savedState)
    model.onQueryChange("Bat")
    advanceTimeBy(300)
    model.onQueryChange("  Batman  ")
    advanceTimeBy(399)
    runCurrent()
    assertTrue(repository.requests.isEmpty())
    advanceTimeBy(1)
    runCurrent()
    assertEquals(listOf("Batman" to 1), repository.requests)
    assertEquals("Batman", model.uiState.value.movies.single().title)
  }

  @Test fun blankSubmissionShowsValidationWithoutRequest() = runTest(dispatcher) {
    val model = SearchViewModel(repository, savedState)
    model.onQueryChange(" \t ")
    model.submitSearch()
    advanceUntilIdle()
    assertTrue(model.uiState.value.showTitleError)
    assertTrue(repository.requests.isEmpty())
    model.onQueryChange("Alien")
    assertFalse(model.uiState.value.showTitleError)
    advanceUntilIdle()
  }

  @Test fun clearCancelsDebounceAndEmptiesResults() = runTest(dispatcher) {
    val model = SearchViewModel(repository, savedState)
    model.onQueryChange("Alien")
    advanceUntilIdle()
    assertFalse(model.uiState.value.movies.isEmpty())
    model.onQueryChange("Aliens")
    advanceTimeBy(200)
    model.onQueryChange("")
    advanceUntilIdle()
    assertEquals(SearchUiState(), model.uiState.value)
    assertEquals(listOf("Alien" to 1), repository.requests)
    assertEquals("", savedState.get<String>("searchQuery"))
  }

  @Test fun staleResponseCannotReplaceNewQuery() = runTest(dispatcher) {
    repository.respond = { title, page ->
      withContext(NonCancellable) { delay(if (title == "Old") 2000 else 10) }
      success(title, page)
    }
    val model = SearchViewModel(repository, savedState)
    model.onQueryChange("Old")
    advanceTimeBy(400)
    runCurrent()
    model.onQueryChange("New")
    advanceUntilIdle()
    assertEquals("New", model.uiState.value.movies.single().title)
    assertFalse(model.uiState.value.isLoading)
  }

  @Test fun clearDuringRequestCannotRestoreOldResults() = runTest(dispatcher) {
    repository.respond = { title, page ->
      withContext(NonCancellable) { delay(1000) }
      success(title, page)
    }
    val model = SearchViewModel(repository, savedState)
    model.onQueryChange("Alien")
    advanceTimeBy(400)
    runCurrent()
    model.onQueryChange("")
    advanceUntilIdle()
    assertEquals(SearchUiState(), model.uiState.value)
  }

  @Test fun submissionRunsImmediatelyAndDoesNotDuplicateRequest() = runTest(dispatcher) {
    val model = SearchViewModel(repository, savedState)
    model.onQueryChange("Alien")
    model.submitSearch()
    runCurrent()
    model.submitSearch()
    advanceUntilIdle()
    assertEquals(listOf("Alien" to 1), repository.requests)
  }

  @Test fun paginationLoads20AtATimeAndStopsAtLastPage() = runTest(dispatcher) {
    repository.respond = { _, page ->
      RepositoryResult.Success(
        MoviePage((1..20).map { movie((page - 1) * 20 + it) }, page, 2, 40),
        DataSource.NETWORK
      )
    }
    val model = SearchViewModel(repository, savedState)
    model.onQueryChange("Movie")
    advanceUntilIdle()
    assertEquals(20, model.uiState.value.movies.size)
    model.loadMore()
    runCurrent()
    model.loadMore()
    advanceUntilIdle()
    assertEquals(40, model.uiState.value.movies.size)
    assertFalse(model.uiState.value.canLoadMore)
    model.loadMore()
    advanceUntilIdle()
    assertEquals(listOf("Movie" to 1, "Movie" to 2), repository.requests)
    model.onQueryChange("New")
    assertTrue(model.uiState.value.movies.isEmpty())
    advanceUntilIdle()
    assertEquals("New" to 1, repository.requests.last())
  }

  @Test fun failedNextPagePreservesResultsAndRetriesSamePage() = runTest(dispatcher) {
    repository.respond = { title, page ->
      if (page == 2) RepositoryResult.Error(ErrorCategory.NETWORK, "Unable to search")
      else success(title, page, totalPages = 2)
    }
    val model = SearchViewModel(repository, savedState)
    model.onQueryChange("Alien")
    advanceUntilIdle()
    model.loadMore()
    advanceUntilIdle()
    assertEquals("Unable to search", model.uiState.value.requestError)
    assertEquals(1, model.uiState.value.movies.size)
    repository.respond = { title, page -> success(title, page, totalPages = 2) }
    model.retry()
    advanceUntilIdle()
    assertEquals(listOf("Alien" to 1, "Alien" to 2, "Alien" to 2), repository.requests)
    assertNull(model.uiState.value.requestError)
    assertEquals(2, model.uiState.value.movies.size)
  }

  @Test fun restoredQueryIsSearched() = runTest(dispatcher) {
    savedState["searchQuery"] = "Alien"
    val model = SearchViewModel(repository, savedState)
    advanceUntilIdle()
    assertEquals("Alien", model.uiState.value.query)
    assertEquals(listOf("Alien" to 1), repository.requests)
  }

  @Test
  fun typingAndRestoringQueryShowsLoadingBeforeCoroutineRuns() =
    runTest(dispatcher) {
      val model = SearchViewModel(repository, savedState)

      model.onQueryChange("Dune")

      assertEquals(
        UiState.Loading,
        model.uiState.value.contentState
      )
      assertTrue(repository.requests.isEmpty())

      model.onQueryChange("")

      assertEquals(
        UiState.Empty,
        model.uiState.value.contentState
      )

      savedState["searchQuery"] = "Alien"

      val restored = SearchViewModel(repository, savedState)

      assertEquals(
        UiState.Loading,
        restored.uiState.value.contentState
      )
    }
}

private fun movie(id: Int, title: String = "Movie $id") = Movie(id, title, null, 2026)

private fun success(title: String, page: Int, totalPages: Int = 1) =
  RepositoryResult.Success(
    MoviePage(listOf(movie(page, title)), page, totalPages, totalPages),
    DataSource.NETWORK
  )

private class SearchRepository : MovieRepository {
  override suspend fun discoverMovies(
    options: ee.movara.app.domain.model.MovieDiscovery,
    page: Int
  ): RepositoryResult<MoviePage> = error("Unused")

  val requests = mutableListOf<Pair<String, Int>>()
  var respond: suspend (String, Int) -> RepositoryResult<MoviePage> =
    { title, page -> success(title, page) }

  override suspend fun searchMovies(title: String, page: Int): RepositoryResult<MoviePage> {
    requests += title to page
    return respond(title, page)
  }

  override suspend fun trendingMovies(page: Int): RepositoryResult<MoviePage> = error("Unused")
  override suspend fun recommendedMovies(movieId: Int, page: Int): RepositoryResult<MoviePage> = error("Unused")
  override suspend fun moviesInGenre(genreId: Int, page: Int): RepositoryResult<MoviePage> = error("Unused")
  override suspend fun movieDetails(movieId: Int): RepositoryResult<MovieDetails> = error("Unused")
  override fun observeMovies(listType: MovieListType): Flow<RepositoryResult<List<Movie>>> = error("Unused")
  override fun observeCount(listType: MovieListType): Flow<RepositoryResult<Int>> = error("Unused")
  override fun observeMembership(movieId: Int, listType: MovieListType): Flow<RepositoryResult<Boolean>> = error("Unused")
  override suspend fun saveMovie(movie: Movie, listType: MovieListType): RepositoryResult<Unit> = error("Unused")
  override suspend fun removeMovie(movieId: Int, listType: MovieListType): RepositoryResult<Unit> = error("Unused")
}
