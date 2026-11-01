package ee.movara.app.data.remote

import kotlinx.serialization.Serializable

/**
 * The cast and crew of one movie. The app reads two things from it: the director,
 * which comes from the crew, and the first few actors, which come from the cast.
 */
@Serializable
data class CreditsDto(
  val id: Int? = null,
  val cast: List<CastMemberDto> = emptyList(),
  val crew: List<CrewMemberDto> = emptyList()
)

/**
 * [order] is TMDB's billing order, where 0 is the lead. TMDB usually returns the cast
 * already sorted, but the mapper sorts by it anyway rather than trusting the order of
 * the array.
 */
@Serializable
data class CastMemberDto(
  val id: Int? = null,
  val name: String? = null,
  val order: Int? = null
)

/**
 * [job] is the exact crew role, for example "Director" or "Screenplay".
 */
@Serializable
data class CrewMemberDto(
  val id: Int? = null,
  val name: String? = null,
  val job: String? = null
)
