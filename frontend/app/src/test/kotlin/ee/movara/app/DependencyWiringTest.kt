package ee.movara.app

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Smoke test for the network dependencies added in issue #20.
 *
 * Retrofit 3.0.0 and Coil 3.6.3 both declare OkHttp 4.12.0, while this project pins OkHttp 5.5.1.
 * Gradle resolves that conflict by upgrading everything to 5.5.1, so this test builds the stack to
 * confirm the libraries still link against each other. The real TMDB client is issue #27.
 */
class DependencyWiringTest {

  @Test
  fun retrofitBuildsOnTopOfOkHttpAndKotlinxSerialization() {
    val loggingInterceptor = HttpLoggingInterceptor()
    loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

    val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()

    val retrofit =
      Retrofit.Builder()
        .baseUrl("https://example.invalid/")
        .client(client)
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()

    assertNotNull(retrofit)
  }
}
