package ee.movara.app.data.repository

/** Home can reuse validated persistent responses; explicit retry and other screens stay network-first. */
enum class MovieRequestPolicy {
  NETWORK_FIRST,
  PREFER_FRESH_CACHE;

  companion object {
    const val FRESH_CACHE_MILLIS = 6 * 60 * 60 * 1000L
  }
}
