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
 * Calls the real TMDB API over the network and parses what comes back.
 *
 * The key comes from `local.properties`. Without one the whole class is skipped, so a clone
 * with no key still has a green test run. These tests need internet access and depend on
 * TMDB being up, which the rest of the suite does not.
 *
 * Run only these:
 * `./gradlew :app:testDebugUnitTest --tests "*TmdbApiServiceLiveTest"`
 */
class TmdbApiServiceLiveTest {

  private val service: TmdbApiService = createTmdbApiService(
    createTmdbOkHttpClient(BuildConfig.TMDB_API_KEY, enableHttpLogging = false)
  )

  @Test
  fun movieDetails_readsAKnownMovieFromTmdb() = runTest {
    assumeTmdbApiKeyIsConfigured()

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
    assumeTmdbApiKeyIsConfigured()

    val page = service.searchMovies(title = "fight club", page = 1)

    assertTrue(page.totalResults > 0)
    assertTrue(page.toDomainMovies().isNotEmpty())
  }

  @Test
  fun trendingMovies_readsThisWeeksListFromTmdb() = runTest {
    assumeTmdbApiKeyIsConfigured()

    val page = service.trendingMovies(page = 1)

    assertTrue(page.toDomainMovies().isNotEmpty())
  }

  private fun assumeTmdbApiKeyIsConfigured() {
    assumeTrue(
      "TMDB_API_KEY is not set, so the live TMDB tests are skipped. " +
        "See section 3 of frontend/README.md.",
      BuildConfig.TMDB_API_KEY.isNotEmpty()
    )
  }
}
