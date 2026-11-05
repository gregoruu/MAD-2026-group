package ee.movara.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/** The query parameter TMDB reads the v3 API key from. */
const val TMDB_API_KEY_QUERY_PARAMETER = "api_key"

/**
 * Adds the TMDB API key to the query string of every request, so no service function has
 * to carry it.
 *
 * With an empty [apiKey] the parameter is still sent and TMDB answers 401, which reaches
 * the screen as a failed request.
 */
class TmdbApiKeyInterceptor(private val apiKey: String) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val urlWithApiKey = request.url
      .newBuilder()
      .setQueryParameter(TMDB_API_KEY_QUERY_PARAMETER, apiKey)
      .build()
    val requestWithApiKey = request
      .newBuilder()
      .url(urlWithApiKey)
      .build()
    return chain.proceed(requestWithApiKey)
  }
}
