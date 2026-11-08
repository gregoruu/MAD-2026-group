package ee.movara.app.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

private const val CONNECT_TIMEOUT_SECONDS = 10L
private const val READ_TIMEOUT_SECONDS = 20L

/**
 * The HTTP client every TMDB request goes through.
 *
 * With [enableHttpLogging] it logs request and response bodies to [httpLogger]; the default
 * is OkHttp's own logger.
 */
fun createTmdbOkHttpClient(
  enableHttpLogging: Boolean,
  httpLogger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT
): OkHttpClient {
  val builder = OkHttpClient.Builder()
    .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)

  if (enableHttpLogging) {
    val loggingInterceptor = HttpLoggingInterceptor(httpLogger)
    loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
    builder.addInterceptor(loggingInterceptor)
  }

  return builder.build()
}

/**
 * Builds the TMDB service on [client]. Responses are parsed with [tmdbJson], the same
 * parser the model tests use.
 *
 * [baseUrl] is the TMDB proxy in the app and a local test server in tests. Service paths
 * are relative to it, so it ends in a slash.
 */
fun createTmdbApiService(
  client: OkHttpClient,
  baseUrl: String
): TmdbApiService {
  val retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(client)
    .addConverterFactory(tmdbJson.asConverterFactory(JSON_MEDIA_TYPE))
    .build()
  return retrofit.create(TmdbApiService::class.java)
}
