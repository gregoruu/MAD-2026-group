package ee.movara.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Search, trending, recommendations and discover-by-genre all return the same shape:
 * a page of movie summaries. One type covers all four endpoints.
 *
 * Every field has a default, including the id, so a response that leaves fields out
 * parses instead of throwing and the mapper drops only the entries it cannot use. See
 * [tmdbJson] for the matching parser settings and for the one case this does not cover,
 * a field of the wrong type.
 */
@Serializable
data class MoviePageDto(
  val page: Int = 1,
  val results: List<MovieSummaryDto> = emptyList(),
  @SerialName("total_pages") val totalPages: Int = 0,
  @SerialName("total_results") val totalResults: Int = 0
)

/**
 * One entry in a list of movies. TMDB omits [posterPath] for movies without artwork
 * and sends [releaseDate] as an empty string for movies without a confirmed date.
 */
@Serializable
data class MovieSummaryDto(
  val id: Int? = null,
  val title: String? = null,
  @SerialName("poster_path") val posterPath: String? = null,
  @SerialName("release_date") val releaseDate: String? = null,
  @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
  @SerialName("vote_average") val voteAverage: Double? = null,
  @SerialName("vote_count") val voteCount: Int? = null
)
