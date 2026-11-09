package ee.movara.app.domain.model

import java.time.LocalDate

/**
 * Movie cards plus optional discovery metadata shared by the quiz and Home.
 * Saved-list entries can lack this metadata; unknown ratings/dates stay null.
 * [posterPath] is the TMDB image path without a size prefix.
 */
data class Movie(
  val id: Int,
  val title: String,
  val posterPath: String?,
  val releaseYear: Int?,
  val genreIds: Set<Int> = emptySet(),
  val rating: Double? = null,
  val voteCount: Int? = null,
  val releaseDate: LocalDate? = null
)
