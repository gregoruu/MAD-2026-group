package ee.movara.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The response of a single movie request. Lesser known movies arrive with most of
 * these fields missing, so every field has a default and the mapper decides what is
 * usable. Nothing here throws on a thin response.
 *
 * [credits] is filled when the request asks TMDB to append credits to the response.
 * When credits are fetched separately instead, it stays null and the mapper takes a
 * [CreditsDto] of its own. Supporting both keeps the choice with the API service.
 */
@Serializable
data class MovieDetailsDto(
  val id: Int? = null,
  val title: String? = null,
  val overview: String? = null,
  val runtime: Int? = null,
  val genres: List<GenreDto> = emptyList(),
  @SerialName("vote_average") val voteAverage: Double? = null,
  @SerialName("vote_count") val voteCount: Int? = null,
  @SerialName("release_date") val releaseDate: String? = null,
  @SerialName("poster_path") val posterPath: String? = null,
  @SerialName("backdrop_path") val backdropPath: String? = null,
  val credits: CreditsDto? = null
)

@Serializable
data class GenreDto(
  val id: Int? = null,
  val name: String? = null
)
