package ee.movara.app

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

/**
 * Checks that the network dependencies added in issue #20 work together over a real request.
 *
 * Retrofit 3.0.0 and Coil 3.6.3 both declare OkHttp 4.12.0, while this project pins OkHttp 5.5.1,
 * so Gradle upgrades them. The risky surface is the request path, where Retrofit and the logging
 * interceptor call into OkHttp types that changed between 4.x and 5.x, so this test drives a
 * request through MockWebServer and deserializes the response instead of only constructing the
 * objects.
 *
 * Not covered: Coil. Its fetcher needs an Android context and a real image, so the OkHttp upgrade
 * stays unverified on Coil's side until issue #48 loads a poster on a device.
 */
class NetworkStackCompatibilityTest {

  private lateinit var server: MockWebServer

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
  }

  @After
  fun tearDown() {
    server.close()
  }

  @Test
  fun retrofitParsesAJsonResponseOverOkHttp() = runTest {
    // "tagline" is not in MovieResponse, so parsing it also proves ignoreUnknownKeys is applied.
    server.enqueue(
      MockResponse(
        code = 200,
        body = """{"id":550,"title":"Fight Club","vote_average":8.4,"tagline":"Mischief."}""",
      )
    )

    val service = movieServiceFor(server)
    val movie = service.movie()

    assertEquals(550, movie.id)
    assertEquals("Fight Club", movie.title)
    assertEquals(8.4, movie.voteAverage, 0.001)

    val recorded = server.takeRequest()
    assertEquals("GET", recorded.method)
    assertEquals("/movie/550", recorded.target)
  }

  private fun movieServiceFor(server: MockWebServer): TestMovieService {
    val loggingInterceptor = HttpLoggingInterceptor()
    loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

    val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    val json = Json { ignoreUnknownKeys = true }

    return Retrofit.Builder()
      .baseUrl(server.url("/"))
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(TestMovieService::class.java)
  }
}

/** Stand-in for the real TMDB service built in issue #27. */
private interface TestMovieService {
  @GET("movie/550") suspend fun movie(): MovieResponse
}

@Serializable
private data class MovieResponse(
  val id: Int,
  val title: String,
  @SerialName("vote_average") val voteAverage: Double,
)
