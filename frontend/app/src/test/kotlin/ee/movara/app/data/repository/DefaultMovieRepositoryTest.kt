package ee.movara.app.data.repository

import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.local.SavedMovieDao
import ee.movara.app.data.local.SavedMovieEntity
import ee.movara.app.data.local.TmdbCacheDao
import ee.movara.app.data.local.TmdbCacheEntity
import ee.movara.app.data.remote.MoviePageDto
import ee.movara.app.data.remote.MovieSummaryDto
import ee.movara.app.domain.model.Movie
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class DefaultMovieRepositoryTest {
  private val api = FakeTmdbApiService()
  private val cache = FakeCacheDao()
  private val saved = FakeSavedMovieDao()

  @Test
  fun successMapsOptionalFieldsAndPersistsResponse() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler)) { 123L }
    val result = repository.searchMovies("  THE   Matrix  ") as RepositoryResult.Success
    assertEquals(DataSource.NETWORK, result.source)
    assertEquals(123L, result.fetchedAt)
    assertEquals(true, result.cachePersisted)
    assertEquals("the matrix", api.lastQuery)
    assertEquals(listOf(Movie(603, "The Matrix", null, null)), result.data.movies)
    assertEquals(1, result.data.page)
    assertEquals(3, result.data.totalPages)
    assertEquals(45, result.data.totalResults)
    assertTrue(cache.entries.containsKey("search:the matrix:1"))
    assertEquals(123L, cache.entries.getValue("search:the matrix:1").fetchedAt)
  }

  @Test
  fun offlineUsesLastSuccessWithoutOverwritingIt() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler)) { 123L }
    val fresh = repository.trendingMovies() as RepositoryResult.Success
    val before = cache.entries.toMap()
    api.failure = IOException("sensitive transport details")
    val offline = repository.trendingMovies() as RepositoryResult.Success
    assertEquals(DataSource.CACHE, offline.source)
    assertEquals(fresh.data, offline.data)
    assertEquals(123L, offline.fetchedAt)
    assertEquals(before, cache.entries)
    assertEquals(2, api.calls)
  }

  @Test
  fun offlineWithoutCacheReturnsError() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler))
    api.failure = SocketTimeoutException("private host")
    val result = repository.trendingMovies() as RepositoryResult.Error
    assertEquals(ErrorCategory.NETWORK, result.category)
    assertFalse(result.message.contains("private host"))
    assertTrue(cache.entries.isEmpty())
  }

  @Test
  fun queriesGenresPagesAndEndpointsAreIndependent() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler))
    repository.searchMovies("Matrix", 1)
    repository.searchMovies("matrix", 2)
    repository.searchMovies("Alien", 1)
    repository.moviesInGenre(28, 1)
    repository.moviesInGenre(28, 2)
    repository.moviesInGenre(35, 1)
    repository.trendingMovies(1)
    repository.recommendedMovies(603, 1)
    repository.recommendedMovies(604, 1)
    repository.movieDetails(603)
    repository.movieDetails(604)
    assertEquals(setOf(
      "search:matrix:1", "search:matrix:2", "search:alien:1",
      "genre:28:1", "genre:28:2", "genre:35:1", "trending:1",
      "recommendations:603:1", "recommendations:604:1", "details:603", "details:604"
    ), cache.entries.keys)
    api.failure = IOException()
    assertEquals(DataSource.CACHE, (repository.searchMovies(" MATRIX ") as RepositoryResult.Success).source)
    assertTrue(repository.searchMovies("Uncached") is RepositoryResult.Error)
    assertTrue(repository.moviesInGenre(99) is RepositoryResult.Error)
    assertTrue(repository.trendingMovies(2) is RepositoryResult.Error)
  }

  @Test
  fun detailsRemainAvailableOffline() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler))
    val fresh = repository.movieDetails(603) as RepositoryResult.Success
    assertNull(fresh.data.runtimeMinutes)
    assertTrue(fresh.data.genres.isEmpty())
    api.failure = IOException()
    val cached = repository.movieDetails(603) as RepositoryResult.Success
    assertEquals(fresh.data, cached.data)
    assertEquals(DataSource.CACHE, cached.source)
  }

  @Test
  fun invalidInputNeverCallsApi() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler))
    val results = listOf(
      repository.searchMovies(" "), repository.searchMovies("Matrix", 0),
      repository.trendingMovies(-1), repository.recommendedMovies(0),
      repository.recommendedMovies(603, 0), repository.moviesInGenre(-1),
      repository.moviesInGenre(28, 0), repository.movieDetails(0),
      repository.saveMovie(Movie(0, "", null, null), MovieListType.LIKED),
      repository.removeMovie(-1, MovieListType.WATCHED),
      repository.observeMembership(0, MovieListType.LIKED).first()
    )
    results.forEach { assertEquals(ErrorCategory.INVALID_INPUT, (it as RepositoryResult.Error).category) }
    assertEquals(0, api.calls)
    assertTrue(cache.entries.isEmpty())
  }

  @Test
  fun validEmptyResponseIsCachedButUnusablePageIsAnError() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler))
    api.pageResponse = MoviePageDto()
    val empty = repository.trendingMovies() as RepositoryResult.Success
    assertTrue(empty.data.movies.isEmpty())
    assertTrue(cache.entries.containsKey("trending:1"))
    api.pageResponse = MoviePageDto(results = listOf(MovieSummaryDto()), totalPages = 1, totalResults = 1)
    val invalid = repository.searchMovies("invalid") as RepositoryResult.Error
    assertEquals(ErrorCategory.INVALID_RESPONSE, invalid.category)
    assertFalse(cache.entries.containsKey("search:invalid:1"))
  }

  @Test
  fun malformedPaginationAndDetailsDoNotReplaceCache() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler))
    repository.trendingMovies()
    repository.movieDetails(603)
    val before = cache.entries.toMap()
    api.pageResponse = api.pageResponse.copy(totalPages = -1)
    assertEquals(DataSource.CACHE, (repository.trendingMovies() as RepositoryResult.Success).source)
    api.detailsResponse = api.detailsResponse.copy(title = null)
    assertEquals(DataSource.CACHE, (repository.movieDetails(603) as RepositoryResult.Success).source)
    assertEquals(before, cache.entries)
  }

  @Test
  fun httpAndSerializationErrorsAreContainedAndRecoverableFailuresUseCache() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler))
    repository.trendingMovies()
    val before = cache.entries.toMap()
    for (failure in listOf(SerializationException("bad JSON"), http(500), http(429), http(408))) {
      api.failure = failure
      assertEquals(DataSource.CACHE, (repository.trendingMovies() as RepositoryResult.Success).source)
      assertTrue(repository.searchMovies("uncached") is RepositoryResult.Error)
    }
    for (status in listOf(401, 403, 404)) {
      api.failure = http(status)
      val error = repository.trendingMovies() as RepositoryResult.Error
      assertEquals(ErrorCategory.HTTP, error.category)
      assertEquals(status, error.httpStatus)
    }
    assertEquals(before, cache.entries)
  }

  @Test
  fun corruptCacheIsNotReturnedAsSuccess() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler))
    api.failure = IOException()
    cache.entries["trending:1"] = TmdbCacheEntity("trending:1", "{broken", 1)
    assertEquals(ErrorCategory.NETWORK, (repository.trendingMovies() as RepositoryResult.Error).category)
  }

  @Test
  fun databaseFailuresAreReportedAndFreshDataIsStillUsable() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler))
    cache.failure = IllegalStateException("disk unavailable")
    val fresh = repository.trendingMovies() as RepositoryResult.Success
    assertEquals(DataSource.NETWORK, fresh.source)
    assertEquals(false, fresh.cachePersisted)
    api.failure = IOException()
    assertEquals(ErrorCategory.DATABASE, (repository.trendingMovies() as RepositoryResult.Error).category)
    saved.failure = IllegalStateException("disk unavailable")
    assertEquals(ErrorCategory.DATABASE,
      (repository.observeLikedMovies().first() as RepositoryResult.Error).category)
    assertEquals(ErrorCategory.DATABASE,
      (repository.saveMovie(Movie(603, "Matrix", null, null), MovieListType.LIKED) as RepositoryResult.Error).category)
    assertEquals(ErrorCategory.DATABASE,
      (repository.removeMovie(603, MovieListType.LIKED) as RepositoryResult.Error).category)
  }

  @Test
  fun cancellationIsRethrownFromApiCacheAndSavedLists() = runTest {
    val repository = DefaultMovieRepository(api, cache, saved, StandardTestDispatcher(testScheduler))
    api.failure = CancellationException("cancel API")
    expectCancellation { repository.trendingMovies() }
    api.failure = null
    cache.failure = CancellationException("cancel write")
    expectCancellation { repository.trendingMovies() }
    api.failure = IOException()
    expectCancellation { repository.trendingMovies() }
    saved.failure = CancellationException("cancel list")
    expectCancellation { repository.observeLikedMovies().first() }
    expectCancellation { repository.saveMovie(Movie(603, "Matrix", null, null), MovieListType.LIKED) }
    expectCancellation { repository.removeMovie(603, MovieListType.LIKED) }
  }

  private suspend fun expectCancellation(block: suspend () -> Any?) {
    try {
      block()
      fail("Expected coroutine cancellation")
    } catch (_: CancellationException) {
      // Required for structured concurrency.
    }
  }

  private fun http(status: Int) = HttpException(Response.error<Any>(status, "{}".toResponseBody()))

  private class FakeCacheDao : TmdbCacheDao {
    val entries = mutableMapOf<String, TmdbCacheEntity>()
    var failure: Exception? = null
    override suspend fun get(key: String): TmdbCacheEntity? {
      failure?.let { throw it }
      return entries[key]
    }
    override suspend fun upsert(entry: TmdbCacheEntity) {
      failure?.let { throw it }
      entries[entry.cacheKey] = entry
    }
  }

  private class FakeSavedMovieDao : SavedMovieDao {
    var failure: Exception? = null
    override fun observeAllMovies(): Flow<List<SavedMovieEntity>> = flow {
      failure?.let { throw it }
      emit(emptyList())
    }
    override fun observeMovies(listType: MovieListType): Flow<List<SavedMovieEntity>> = flow {
      failure?.let { throw it }
      emit(emptyList())
    }
    override fun observeCount(listType: MovieListType) = flowOf(0)
    override fun observeMembership(movieId: Int, listType: MovieListType) = flowOf(false)
    override suspend fun upsert(movie: SavedMovieEntity) { failure?.let { throw it } }
    override suspend fun remove(tmdbId: Int, listType: MovieListType) { failure?.let { throw it } }
  }
}
