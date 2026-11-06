package ee.movara.app.data.remote

import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

private const val TEST_API_KEY = "test-api-key"

private const val EMPTY_PAGE = """{ "page": 1, "results": [], "total_pages": 0, "total_results": 0 }"""

/**
 * Drives every service function through a local server and checks the request that leaves
 * the client and the response that comes back.
 *
 * What the response is turned into afterwards is covered by the mapping tests.
 */
class TmdbApiServiceTest {

  private lateinit var server: MockWebServer
  private lateinit var service: TmdbApiService

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    service = serviceFor(createTmdbOkHttpClient(TEST_API_KEY, enableHttpLogging = false))
  }

  @After
  fun tearDown() {
    server.close()
  }

  @Test
  fun searchMovies_asksForTheTitleAndPage() = runTest {
    server.enqueue(MockResponse(code = 200, body = EMPTY_PAGE))

    service.searchMovies(title = "blade runner", page = 2)

    val request = server.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("/search/movie", request.url.encodedPath)
    assertEquals("blade runner", request.url.queryParameter("query"))
    assertEquals("2", request.url.queryParameter("page"))
  }

  @Test
  fun searchMovies_parsesThePageOfResults() = runTest {
    val body = """
      {
        "page": 1,
        "results": [
          { "id": 603, "title": "The Matrix", "poster_path": "/matrix.jpg", "release_date": "1999-03-30" }
        ],
        "total_pages": 1,
        "total_results": 1
      }
    """.trimIndent()
    server.enqueue(MockResponse(code = 200, body = body))

    val page = service.searchMovies(title = "matrix", page = 1)

    assertEquals(1, page.page)
    assertEquals(1, page.totalPages)
    assertEquals(1, page.results.size)
    assertEquals(603, page.results[0].id)
    assertEquals("The Matrix", page.results[0].title)
  }

  @Test
  fun trendingMovies_asksForThisWeek() = runTest {
    server.enqueue(MockResponse(code = 200, body = EMPTY_PAGE))

    service.trendingMovies(page = 1)

    val request = server.takeRequest()
    assertEquals("/trending/movie/week", request.url.encodedPath)
    assertEquals("1", request.url.queryParameter("page"))
  }

  @Test
  fun recommendedMovies_asksForTheRecommendationsOfOneMovie() = runTest {
    server.enqueue(MockResponse(code = 200, body = EMPTY_PAGE))

    service.recommendedMovies(movieId = 550, page = 3)

    val request = server.takeRequest()
    assertEquals("/movie/550/recommendations", request.url.encodedPath)
    assertEquals("3", request.url.queryParameter("page"))
  }

  @Test
  fun moviesInGenre_asksForTheGenreSortedByPopularity() = runTest {
    server.enqueue(MockResponse(code = 200, body = EMPTY_PAGE))

    service.moviesInGenre(genreId = 878, page = 1)

    val request = server.takeRequest()
    assertEquals("/discover/movie", request.url.encodedPath)
    assertEquals("878", request.url.queryParameter("with_genres"))
    assertEquals("popularity.desc", request.url.queryParameter("sort_by"))
    assertEquals("1", request.url.queryParameter("page"))
  }

  @Test
  fun movieDetails_asksForTheMovieWithItsCreditsAppended() = runTest {
    server.enqueue(MockResponse(code = 200, body = """{ "id": 550, "title": "Fight Club" }"""))

    service.movieDetails(movieId = 550)

    val request = server.takeRequest()
    assertEquals("/movie/550", request.url.encodedPath)
    assertEquals("credits", request.url.queryParameter("append_to_response"))
  }

  @Test
  fun movieDetails_parsesTheMovieAndTheAppendedCredits() = runTest {
    val body = """
      {
        "id": 550,
        "title": "Fight Club",
        "runtime": 139,
        "genres": [ { "id": 18, "name": "Drama" } ],
        "vote_average": 8.4,
        "credits": {
          "cast": [ { "id": 819, "name": "Edward Norton", "order": 0 } ],
          "crew": [ { "id": 7467, "name": "David Fincher", "job": "Director" } ]
        }
      }
    """.trimIndent()
    server.enqueue(MockResponse(code = 200, body = body))

    val details = service.movieDetails(movieId = 550)

    assertEquals("Fight Club", details.title)
    assertEquals(139, details.runtime)
    assertEquals("Drama", details.genres[0].name)
    assertNotNull(details.credits)
    assertEquals("Edward Norton", details.credits?.cast?.get(0)?.name)
    assertEquals("David Fincher", details.credits?.crew?.get(0)?.name)
  }

  @Test
  fun everyRequest_carriesTheApiKey() = runTest {
    for (index in 1..4) {
      server.enqueue(MockResponse(code = 200, body = EMPTY_PAGE))
    }
    server.enqueue(MockResponse(code = 200, body = """{ "id": 550, "title": "Fight Club" }"""))

    service.searchMovies(title = "matrix", page = 1)
    service.trendingMovies(page = 1)
    service.recommendedMovies(movieId = 550, page = 1)
    service.moviesInGenre(genreId = 28, page = 1)
    service.movieDetails(movieId = 550)

    val requests = mutableListOf<RecordedRequest>()
    for (index in 1..5) {
      requests.add(server.takeRequest())
    }
    for (request in requests) {
      assertEquals(request.url.encodedPath, TEST_API_KEY, request.url.queryParameter("api_key"))
    }
  }

  @Test
  fun request_failsWhenTmdbAnswersWithAnError() = runTest {
    server.enqueue(
      MockResponse(
        code = 401,
        body = """{ "status_code": 7, "status_message": "Invalid API key." }"""
      )
    )

    var thrown: HttpException? = null
    try {
      service.trendingMovies(page = 1)
    } catch (exception: HttpException) {
      thrown = exception
    }

    assertNotNull(thrown)
    assertEquals(401, thrown?.code())
  }

  @Test
  fun request_failsWithAnIoExceptionWhenTheServerCannotBeReached() = runTest {
    val unreachableServer = MockWebServer()
    unreachableServer.start()
    val unreachableUrl = unreachableServer.url("/").toString()
    unreachableServer.close()
    val serviceOnAClosedPort = createTmdbApiService(
      createTmdbOkHttpClient(TEST_API_KEY, enableHttpLogging = false),
      unreachableUrl
    )

    var thrown: IOException? = null
    try {
      serviceOnAClosedPort.trendingMovies(page = 1)
    } catch (exception: IOException) {
      thrown = exception
    }

    assertNotNull(thrown)
  }

  @Test
  fun request_failsWhenTheResponseCannotBeParsed() = runTest {
    server.enqueue(MockResponse(code = 200, body = """{ "page": "not a number" }"""))

    var thrown: SerializationException? = null
    try {
      service.trendingMovies(page = 1)
    } catch (exception: SerializationException) {
      thrown = exception
    }

    assertNotNull(thrown)
  }

  @Test
  fun client_hasNoLoggingInterceptorWhenLoggingIsOff() {
    val client = createTmdbOkHttpClient(TEST_API_KEY, enableHttpLogging = false)

    assertNull(loggingInterceptorOf(client))
  }

  @Test
  fun client_hasALoggingInterceptorWhenLoggingIsOn() {
    val client = createTmdbOkHttpClient(TEST_API_KEY, enableHttpLogging = true)

    assertNotNull(loggingInterceptorOf(client))
  }

  @Test
  fun httpLogging_logsTheRequestWithoutTheApiKey() = runTest {
    server.enqueue(MockResponse(code = 200, body = EMPTY_PAGE))
    val logger = RecordingLogger()
    val loggingService = serviceFor(
      createTmdbOkHttpClient(TEST_API_KEY, enableHttpLogging = true, httpLogger = logger)
    )

    loggingService.searchMovies(title = "matrix", page = 1)

    val log = logger.lines.joinToString(separator = "\n")
    assertTrue(log, log.contains("/search/movie"))
    assertFalse(log, log.contains(TEST_API_KEY))
  }

  private fun serviceFor(client: OkHttpClient): TmdbApiService {
    return createTmdbApiService(client, server.url("/").toString())
  }

  private fun loggingInterceptorOf(client: OkHttpClient): HttpLoggingInterceptor? {
    for (interceptor in client.interceptors) {
      if (interceptor is HttpLoggingInterceptor) {
        return interceptor
      }
    }
    return null
  }
}

/** Keeps the log lines in memory so a test can read them. */
private class RecordingLogger : HttpLoggingInterceptor.Logger {

  val lines = mutableListOf<String>()

  override fun log(message: String) {
    lines.add(message)
  }
}
