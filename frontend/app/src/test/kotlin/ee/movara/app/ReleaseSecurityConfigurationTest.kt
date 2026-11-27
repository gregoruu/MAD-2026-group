package ee.movara.app

import ee.movara.app.data.remote.createTmdbApiService
import ee.movara.app.data.remote.createTmdbOkHttpClient
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/** Uses the generated release constants, so changing the Gradle build type fails CI. */
class ReleaseSecurityConfigurationTest {

  @Before
  fun requireReleaseVariant() {
    assumeTrue(BuildConfig.BUILD_TYPE == "release")
  }

  @Test
  fun releaseDisablesDebuggingAndHttpLogging() = runTest {
    assertFalse(BuildConfig.DEBUG)
    assertFalse(BuildConfig.ENABLE_HTTP_LOGGING)

    val logLines = mutableListOf<String>()
    val client = createTmdbOkHttpClient(
      enableHttpLogging = BuildConfig.ENABLE_HTTP_LOGGING,
      httpLogger = HttpLoggingInterceptor.Logger { logLines.add(it) }
    )
    MockWebServer().use { server ->
      server.start()
      server.enqueue(MockResponse(
        code = 200,
        body = """{ "page": 1, "results": [], "total_pages": 0, "total_results": 0 }"""
      ))
      val service = createTmdbApiService(client, server.url("/").toString())

      service.searchMovies(title = "private search", page = 1)

      assertTrue("Release requests must not be logged", logLines.isEmpty())
      assertEquals("/search/movie", server.takeRequest().url.encodedPath)
    }
  }

  @Test
  fun releaseUsesTheProductionHttpsProxyWithoutCredentials() {
    val url = BuildConfig.TMDB_BASE_URL.toHttpUrl()

    assertTrue(url.isHttps)
    assertEquals("movara-tmdb-proxy-production.movara.workers.dev", url.host)
    assertEquals("/", url.encodedPath)
    assertEquals("", url.username)
    assertEquals("", url.password)
    assertEquals(0, url.querySize)
  }
}
