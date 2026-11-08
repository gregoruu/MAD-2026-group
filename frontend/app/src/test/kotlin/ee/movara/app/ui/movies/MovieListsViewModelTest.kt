package ee.movara.app.ui.movies

import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.*
import ee.movara.app.domain.model.*
import ee.movara.app.ui.home.HomeViewModel
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
  private val movie = Movie(603, "The Matrix", null, 1999)
  private val repository = ListsRepository()

  @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
  @After fun tearDown() { Dispatchers.resetMain() }

  @Test fun watchedEmissionFiltersHomeWithoutAnotherNetworkRequest() = runTest {
    repository.movies = listOf(movie, movie)
    val lists = MovieListsViewModel(repository)
    val home = HomeViewModel(repository, lists.uiState)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { home.uiState.collect() }
    advanceUntilIdle()
    assertEquals(listOf(movie), home.uiState.value.movies)
    repository.snapshots.value = RepositoryResult.Success(MovieLists(watched = listOf(movie)), DataSource.LOCAL)
    advanceUntilIdle()
    assertTrue(home.uiState.value.movies.isEmpty())
    assertEquals(1, repository.trendingCalls)
    repository.snapshots.value = RepositoryResult.Success(MovieLists(), DataSource.LOCAL)
    advanceUntilIdle()
    assertEquals(listOf(movie), home.uiState.value.movies)
    assertEquals(1, repository.trendingCalls)
  }

  @Test fun failedWritePreservesSnapshotAndBlocksActionsUntilRetry() = runTest {
    val lists = MovieListsViewModel(repository)
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
    val lists = MovieListsViewModel(repository)
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
    repository.snapshots.value = RepositoryResult.Error(ErrorCategory.DATABASE, "Storage unavailable")
    repository.remoteError = true
    val lists = MovieListsViewModel(repository)
    val home = HomeViewModel(repository, lists.uiState)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { home.uiState.collect() }
    advanceUntilIdle()
    assertNotNull(lists.uiState.value.storageError)
    assertNotNull(home.uiState.value.error)
    repository.snapshots.value = RepositoryResult.Success(MovieLists(), DataSource.LOCAL)
    repository.remoteError = false
    repository.movies = listOf(movie)
    lists.retry()
    home.refresh()
    advanceUntilIdle()
    assertNull(lists.uiState.value.storageError)
    assertNull(home.uiState.value.error)
    assertEquals(listOf(movie), home.uiState.value.movies)
    assertEquals(2, repository.trendingCalls)
  }
}

private class ListsRepository : MovieRepository {
  val snapshots = MutableStateFlow<RepositoryResult<MovieLists>>(RepositoryResult.Success(MovieLists(), DataSource.LOCAL))
  var movies = emptyList<Movie>()
  var trendingCalls = 0
  var writes = 0
  var remoteError = false
  var writeResult: RepositoryResult<Unit> = RepositoryResult.Success(Unit, DataSource.LOCAL)
  override fun observeLists(): Flow<RepositoryResult<MovieLists>> = snapshots
  override suspend fun trendingMovies(page: Int): RepositoryResult<MoviePage> {
    trendingCalls++
    return if (remoteError) unused() else RepositoryResult.Success(MoviePage(movies, 1, 1, movies.size), DataSource.NETWORK)
  }
  override suspend fun saveMovie(movie: Movie, listType: MovieListType): RepositoryResult<Unit> { writes++; return writeResult }
  override suspend fun removeMovie(movieId: Int, listType: MovieListType): RepositoryResult<Unit> { writes++; return writeResult }
  private fun unused() = RepositoryResult.Error(ErrorCategory.UNEXPECTED, "Unavailable")
  override suspend fun searchMovies(title: String, page: Int): RepositoryResult<MoviePage> = unused()
  override suspend fun recommendedMovies(movieId: Int, page: Int): RepositoryResult<MoviePage> = unused()
  override suspend fun moviesInGenre(genreId: Int, page: Int): RepositoryResult<MoviePage> = unused()
  override suspend fun movieDetails(movieId: Int): RepositoryResult<MovieDetails> = unused()
  override fun observeMovies(listType: MovieListType): Flow<RepositoryResult<List<Movie>>> = flowOf(unused())
  override fun observeCount(listType: MovieListType): Flow<RepositoryResult<Int>> = flowOf(unused())
  override fun observeMembership(movieId: Int, listType: MovieListType): Flow<RepositoryResult<Boolean>> = flowOf(unused())
}
