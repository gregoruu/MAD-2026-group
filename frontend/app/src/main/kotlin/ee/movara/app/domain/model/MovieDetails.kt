package ee.movara.app.domain.model

/**
 * Everything the Movie Details screen shows: title, year, runtime, rating, genres,
 * director, description and the first few actors.
 *
 * TMDB leaves a lot of this out for lesser known movies. The design says a missing
 * runtime or cast is simply not shown and a missing description is replaced with
 * "No description available", so those fields are nullable or empty here rather than
 * carrying a placeholder. Deciding what to display is the screen's job.
 *
 * [rating] is TMDB's average vote. It is null when nobody has voted yet, so the
 * screen does not show a confident looking "0.0".
 */
data class MovieDetails(
  val id: Int,
  val title: String,
  val releaseYear: Int?,
  val runtimeMinutes: Int?,
  val rating: Double?,
  val genres: List<Genre>,
  val director: String?,
  val overview: String?,
  val posterPath: String?,
  val backdropPath: String?,
  val topCast: List<String>
)
