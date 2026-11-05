package ee.movara.app.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** The root of TMDB API v3. Service paths are relative to it, so it ends in a slash. */
const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

private const val CONNECT_TIMEOUT_SECONDS = 10L
private const val READ_TIMEOUT_SECONDS = 20L

/**
 * The HTTP client every TMDB request goes through.
 *
 * It holds the API key interceptor, and with [enableHttpLogging] it also logs request and
 * response bodies. The log redacts the value of the `api_key` parameter, so the key does
 * not end up in logcat.
 *
 * [httpLogger] is where those lines go; the default is OkHttp's own logger.
 */
fun createTmdbOkHttpClient(
  apiKey: String,
  enableHttpLogging: Boolean,
  httpLogger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT
): OkHttpClient {
  val builder = OkHttpClient.Builder()
    .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .addInterceptor(TmdbApiKeyInterceptor(apiKey))

  if (enableHttpLogging) {
    val loggingInterceptor = HttpLoggingInterceptor(httpLogger)
    loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
    loggingInterceptor.redactQueryParams(TMDB_API_KEY_QUERY_PARAMETER)
    builder.addInterceptor(loggingInterceptor)
  }

  return builder.build()
}

/**
 * Builds the TMDB service on [client]. Responses are parsed with [tmdbJson], the same
 * parser the model tests use.
 *
 * [baseUrl] is TMDB itself in the app and a local test server in tests.
 */
fun createTmdbApiService(
  client: OkHttpClient,
  baseUrl: String = TMDB_BASE_URL
): TmdbApiService {
  val retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(client)
    .addConverterFactory(tmdbJson.asConverterFactory(JSON_MEDIA_TYPE))
    .build()
  return retrofit.create(TmdbApiService::class.java)
}
