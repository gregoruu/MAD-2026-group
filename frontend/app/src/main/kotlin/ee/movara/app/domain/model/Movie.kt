package ee.movara.app.domain.model

/**
 * A movie as every list in the app shows it: the Spotlight card and recommendation
 * rails on Home, Search results, the selected genre list and My Movies. All of those
 * show a poster, a title and a year, so they need nothing more.
 *
 * The Movie Details screen uses [MovieDetails] instead.
 *
 * The fields here are the ones the design specification asks a list entry to show:
 * poster, title and year. If the Home Top Pick turns out to need more than that, it is
 * one movie, so fetching its [MovieDetails] is cheaper than widening this model and the
 * summary response for every list in the app. That call belongs to the Home ticket.
 *
 * [posterPath] is the path TMDB returns, for example "/abc123.jpg". The size prefix
 * is chosen where the image is displayed, because the design uses a different size
 * per element.
 */
data class Movie(
  val id: Int,
  val title: String,
  val posterPath: String?,
  val releaseYear: Int?
)
