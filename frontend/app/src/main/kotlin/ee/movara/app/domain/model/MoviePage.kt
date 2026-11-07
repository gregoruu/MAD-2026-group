package ee.movara.app.domain.model

/** Pagination metadata remains available even when individual unusable cards are dropped. */
data class MoviePage(
  val movies: List<Movie>,
  val page: Int,
  val totalPages: Int,
  val totalResults: Int
)
