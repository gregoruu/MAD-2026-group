package ee.movara.app.ui.movies

import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.*
import ee.movara.app.domain.model.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import ee.movara.app.ui.home.HomeViewModel
import ee.movara.app.ui.moviedetails.MovieDetailsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieListsViewModelTest {
  private val models = mutableListOf<ViewModel>()
  private fun lists() = MovieListsViewModel(repository).also { models += it }
  private fun home(lists: MovieListsViewModel) = HomeViewModel(repository, lists).also { models += it }
  private val movie = Movie(603, "The Matrix", null, 1999)
  private val repository = ListsRepository()

  @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
  @After fun tearDown() { models.forEach { it.viewModelScope.cancel() }; Dispatchers.resetMain() }

  @Test fun watchedEmissionFiltersHomeWithoutAnotherNetworkRequest() = runTest {
    repository.movies = listOf(movie, movie)
    val lists = lists()
    val home = home(lists)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { home.uiState.collect() }
    advanceUntilIdle()
    assertEquals(listOf(movie), home.uiState.value.rails.flatMap { it.movies })
    repository.snapshots.value = RepositoryResult.Success(MovieLists(watched = listOf(movie)), DataSource.LOCAL)
    advanceUntilIdle()
    assertTrue(home.uiState.value.rails.flatMap { it.movies }.isEmpty())
    assertEquals(1, repository.trendingCalls)
    repository.snapshots.value = RepositoryResult.Success(MovieLists(), DataSource.LOCAL)
    advanceUntilIdle()
    assertEquals(listOf(movie), home.uiState.value.rails.flatMap { it.movies })
    assertEquals(1, repository.trendingCalls)
  }

  @Test fun failedWritePreservesSnapshotAndBlocksActionsUntilRetry() = runTest {
    val lists = lists()
    advanceUntilIdle()
    repository.writeResult = RepositoryResult.Error(ErrorCategory.DATABASE, "Storage unavailable")
    lists.setMembership(movie, MovieListType.WATCHED, true)
    advanceUntilIdle()
    assertEquals(MovieLists(), lists.uiState.value.lists)
    assertEquals("Storage unavailable", lists.uiState.value.storageError)
    assertFalse(lists.uiState.value.actionsEnabled)
    assertTrue(lists.uiState.value.pendingIds.isEmpty())
    lists.setMembership(movie, MovieListType.LIKED, true)
    advanceUntilIdle()
    assertEquals(1, repository.writes)
    lists.retry()
    advanceUntilIdle()
    assertTrue(lists.uiState.value.actionsEnabled)
    assertNull(lists.uiState.value.storageError)
  }

  @Test fun successfulWriteWaitsForRoomConfirmationAndIgnoresRepeatedTap() = runTest {
    val lists = lists()
    advanceUntilIdle()
    lists.setMembership(movie, MovieListType.WATCHED, true)
    advanceUntilIdle()
    assertTrue(movie.id in lists.uiState.value.pendingIds)
    assertFalse(lists.uiState.value.lists.contains(movie.id, MovieListType.WATCHED))
    lists.setMembership(movie, MovieListType.WATCHED, true)
    advanceUntilIdle()
    assertEquals(1, repository.writes)
    repository.snapshots.value = RepositoryResult.Success(MovieLists(watched = listOf(movie)), DataSource.LOCAL)
    advanceUntilIdle()
    assertTrue(lists.uiState.value.pendingIds.isEmpty())
    assertTrue(lists.uiState.value.lists.contains(movie.id, MovieListType.WATCHED))
  }

  @Test fun simultaneousStorageAndNetworkErrorsCanBothRecover() = runTest {
    repository.remoteError = true
    val lists = lists()
    val home = home(lists)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { home.uiState.collect() }
    advanceUntilIdle()
    assertTrue(home.uiState.value.rails.single().failed)
    repository.snapshots.value = RepositoryResult.Error(ErrorCategory.DATABASE, "Storage unavailable")
    advanceUntilIdle()
    assertNotNull(lists.uiState.value.storageError)
    assertTrue(home.uiState.value.loadFailed)
    assertTrue(home.uiState.value.rails.isEmpty())
    repository.snapshots.value = RepositoryResult.Success(MovieLists(), DataSource.LOCAL)
    repository.remoteError = false
    repository.movies = listOf(movie)
    lists.retry()
    home.retry()
    advanceUntilIdle()
    assertNull(lists.uiState.value.storageError)
    assertFalse(home.uiState.value.loadFailed)
    assertEquals(listOf(movie), home.uiState.value.rails.flatMap { it.movies })
    assertEquals(2, repository.trendingCalls)
  }

  @Test fun recommendationsWaitForTheFirstSavedListsSnapshot() = runTest {
    val gate = CompletableDeferred<Unit>()
    repository.storageGate = gate
    repository.movies = listOf(movie)
    repository.snapshots.value = RepositoryResult.Success(MovieLists(watched = listOf(movie)), DataSource.LOCAL)
    val lists = lists()
    val home = home(lists)
    advanceUntilIdle()
    assertTrue(home.uiState.value.loading)
    assertTrue(home.uiState.value.rails.isEmpty())
    assertNull(home.uiState.value.topPick)
    assertEquals(0, repository.trendingCalls)
    lists.setMembership(movie, MovieListType.LIKED, true)
    assertEquals(0, repository.writes)

    gate.complete(Unit)
    advanceUntilIdle()
    assertFalse(home.uiState.value.loading)
    assertTrue(home.uiState.value.rails.single().movies.isEmpty())
    assertNull(home.uiState.value.topPick)
  }

  @Test fun networkFinishingDuringStorageReloadCannotExposeWatchedMovies() = runTest {
    val network = CompletableDeferred<Unit>()
    repository.networkGate = network
    repository.movies = listOf(movie)
    val lists = lists()
    val home = home(lists)
    val displayedPicks = mutableListOf<Movie>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      home.uiState.collect { state -> state.topPick?.let(displayedPicks::add) }
    }
    advanceUntilIdle()
    assertEquals(1, repository.trendingCalls)

    val storage = CompletableDeferred<Unit>()
    repository.storageGate = storage
    lists.retry()
    network.complete(Unit)
    advanceUntilIdle()
    assertTrue(home.uiState.value.loading)
    assertTrue(home.uiState.value.rails.isEmpty())
    assertNull(home.uiState.value.topPick)

    repository.snapshots.value = RepositoryResult.Success(MovieLists(watched = listOf(movie)), DataSource.LOCAL)
    storage.complete(Unit)
    advanceUntilIdle()
    assertFalse(home.uiState.value.loading)
    assertTrue(home.uiState.value.rails.single().movies.isEmpty())
    assertEquals(1, repository.trendingCalls)
    assertTrue(displayedPicks.isEmpty())
  }

  @Test fun watchedFromDetailsUpdatesSharedListsAndHomeOnlyAfterRoomConfirmation() = runTest {
    repository.movies = listOf(movie)
    val lists = lists()
    val home = home(lists)
    val details = MovieDetailsViewModel(movie.id, repository, lists).also { models += it }
    advanceUntilIdle()
    details.toggleWatched()
    advanceUntilIdle()
    assertTrue(movie.id in lists.uiState.value.pendingIds)
    assertFalse(details.uiState.value.actionsEnabled)
    assertFalse(details.uiState.value.isWatched)
    assertEquals(movie, home.uiState.value.topPick)

    repository.snapshots.value = RepositoryResult.Success(MovieLists(watched = listOf(movie)), DataSource.LOCAL)
    advanceUntilIdle()
    assertTrue(details.uiState.value.isWatched)
    assertTrue(details.uiState.value.actionsEnabled)
    assertTrue(lists.uiState.value.pendingIds.isEmpty())
    assertNull(home.uiState.value.topPick)
    assertEquals(1, repository.trendingCalls)

    repository.snapshots.value = RepositoryResult.Error(ErrorCategory.DATABASE, "Storage unavailable")
    advanceUntilIdle()
    assertFalse(details.uiState.value.actionsEnabled)
    assertTrue(home.uiState.value.loadFailed)
    assertTrue(home.uiState.value.rails.isEmpty())
    assertEquals("Storage unavailable", details.uiState.value.actionError)
  }
}

private class ListsRepository : MovieRepository {
  val snapshots = MutableStateFlow<RepositoryResult<MovieLists>>(RepositoryResult.Success(MovieLists(), DataSource.LOCAL))
  var movies = emptyList<Movie>()
  var trendingCalls = 0
  var writes = 0
  var remoteError = false
  var storageGate: CompletableDeferred<Unit>? = null
  var networkGate: CompletableDeferred<Unit>? = null
  var writeResult: RepositoryResult<Unit> = RepositoryResult.Success(Unit, DataSource.LOCAL)
  override fun observeLists(): Flow<RepositoryResult<MovieLists>> = flow {
    storageGate?.await()
    emitAll(snapshots)
  }
  override suspend fun trendingMovies(page: Int): RepositoryResult<MoviePage> {
    trendingCalls++
    networkGate?.await()
    return if (remoteError) unused() else RepositoryResult.Success(MoviePage(movies, 1, 1, movies.size), DataSource.NETWORK)
  }
  override suspend fun saveMovie(movie: Movie, listType: MovieListType): RepositoryResult<Unit> { writes++; return writeResult }
  override suspend fun removeMovie(movieId: Int, listType: MovieListType): RepositoryResult<Unit> { writes++; return writeResult }
  private fun unused() = RepositoryResult.Error(ErrorCategory.UNEXPECTED, "Unavailable")
  override suspend fun searchMovies(title: String, page: Int): RepositoryResult<MoviePage> = unused()
  override suspend fun recommendedMovies(movieId: Int, page: Int): RepositoryResult<MoviePage> = unused()
  override suspend fun moviesInGenre(genreId: Int, page: Int): RepositoryResult<MoviePage> = unused()
  override suspend fun movieDetails(movieId: Int): RepositoryResult<MovieDetails> = RepositoryResult.Success(
    MovieDetails(movieId, "The Matrix", 1999, null, null, emptyList(), null, null, null, null, emptyList()),
    DataSource.NETWORK
  )
  override fun observeMovies(listType: MovieListType): Flow<RepositoryResult<List<Movie>>> = flowOf(unused())
  override fun observeCount(listType: MovieListType): Flow<RepositoryResult<Int>> = flowOf(unused())
  override fun observeMembership(movieId: Int, listType: MovieListType): Flow<RepositoryResult<Boolean>> = flowOf(unused())
}
