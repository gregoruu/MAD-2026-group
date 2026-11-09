package ee.movara.app.domain.model

import java.time.LocalDate

/** Shared discovery options for the quiz and personalized Home recommendations. */
data class MovieDiscovery(
  val genreId: Int? = null,
  val sort: MovieSort = MovieSort.POPULARITY,
  val minimumRating: Int? = null,
  val minimumVotes: Int? = null,
  val releasedBy: LocalDate? = null
) {
  companion object {
    /** Recognizable starting films, using simple TMDB filters rather than a local score. */
    fun familiar(genreId: Int?, releasedBy: LocalDate) = MovieDiscovery(
      genreId = genreId,
      sort = MovieSort.MOST_VOTED,
      minimumRating = 6,
      minimumVotes = 100,
      releasedBy = releasedBy
    )
  }
}

enum class MovieSort { POPULARITY, MOST_VOTED, HIGHEST_RATED }
