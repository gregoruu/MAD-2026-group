package ee.movara.app.data.remote

import ee.movara.app.BuildConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Fight Club. Its TMDB entry is complete and its id does not change. */
private const val KNOWN_MOVIE_ID = 550

/**
 * Set by the build when the live tests are asked for. See `frontend/app/build.gradle.kts`.
 */
private const val LIVE_TESTS_SYSTEM_PROPERTY = "movara.runLiveTmdbTests"

/**
 * Calls the real TMDB API through the proxy at [BuildConfig.TMDB_BASE_URL] and parses what
 * comes back.
 *
 * These tests need internet access and depend on the proxy and TMDB being up, which the rest
 * of the suite does not, so they do not run unless somebody asks for them. The whole class
 * is skipped unless the build was started with `-PrunLiveTmdbTests`, which keeps a plain
 * `./gradlew test` free of the network.
 *
 * Run only these:
 * `./gradlew :app:testDebugUnitTest -PrunLiveTmdbTests --tests "*TmdbApiServiceLiveTest"`
 */
class TmdbApiServiceLiveTest {

  private val service: TmdbApiService = createTmdbApiService(
    createTmdbOkHttpClient(enableHttpLogging = false),
    BuildConfig.TMDB_BASE_URL
  )

  @Test
  fun movieDetails_readsAKnownMovieFromTmdb() = runTest {
    assumeLiveTmdbTestsAreEnabled()

    val details = service.movieDetails(movieId = KNOWN_MOVIE_ID)

    assertEquals(KNOWN_MOVIE_ID, details.id)
    assertEquals("Fight Club", details.title)
    assertNotNull(details.credits)
    assertTrue(details.genres.isNotEmpty())

    val movie = details.toDomainOrNull()
    assertNotNull(movie)
    assertEquals("David Fincher", movie?.director)
    assertTrue(movie?.topCast?.isNotEmpty() == true)
  }

  @Test
  fun searchMovies_readsAPageOfResultsFromTmdb() = runTest {
    assumeLiveTmdbTestsAreEnabled()

    val page = service.searchMovies(title = "fight club", page = 1)

    assertTrue(page.totalResults > 0)
    assertTrue(page.toDomainMovies().isNotEmpty())
  }

  @Test
  fun trendingMovies_readsThisWeeksListFromTmdb() = runTest {
    assumeLiveTmdbTestsAreEnabled()

    val page = service.trendingMovies(page = 1)

    assertTrue(page.toDomainMovies().isNotEmpty())
  }

  /** Skips the test unless the live tests were asked for. */
  private fun assumeLiveTmdbTestsAreEnabled() {
    assumeTrue(
      "The live TMDB tests are off. Run them with " +
        "`./gradlew :app:testDebugUnitTest -PrunLiveTmdbTests " +
        "--tests \"*TmdbApiServiceLiveTest\"`. See section 3 of frontend/README.md.",
      System.getProperty(LIVE_TESTS_SYSTEM_PROPERTY).toBoolean()
    )
  }
}
