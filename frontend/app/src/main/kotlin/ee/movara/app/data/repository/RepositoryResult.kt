package ee.movara.app.data.repository

/** No transport exceptions or database entities cross the repository boundary. */
sealed interface RepositoryResult<out T> {
  data class Success<T>(
    val data: T,
    val source: DataSource,
    /** Time of the successful TMDB fetch; null for user lists. CACHE data may be stale. */
    val fetchedAt: Long? = null,
    /** False means fresh data is usable but could not be persisted for offline use. */
    val cachePersisted: Boolean? = null,
    /** A validated, recent response reused before attempting the network, rather than an error fallback. */
    val freshCacheHit: Boolean = false
  ) : RepositoryResult<T>

  data class Error(
    val category: ErrorCategory,
    val message: String,
    val httpStatus: Int? = null
  ) : RepositoryResult<Nothing>
}

enum class DataSource { NETWORK, CACHE, LOCAL }

enum class ErrorCategory { INVALID_INPUT, NETWORK, HTTP, INVALID_RESPONSE, DATABASE, UNEXPECTED }
