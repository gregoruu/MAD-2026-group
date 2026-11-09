package ee.movara.app.data.remote

import ee.movara.app.domain.model.Genre
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieDetails
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Turns TMDB responses into the app's own models.
 *
 * These functions live next to the data transfer objects because `domain/model` must
 * not know about the network layer. Repositories call them; nothing above the
 * repository layer sees a data transfer object.
 *
 * The rule throughout is that a thin response degrades instead of throwing. A field TMDB
 * left out becomes null or an empty list, and an entry the app could not show at all is
 * dropped from the list rather than rendered blank, so one unusable result does not cost
 * the user the other nineteen on the page.
 *
 * That rule covers missing and null fields, which is what TMDB actually does. It does
 * not cover a field of the wrong type: `"id": "invalid"` fails while the response is
 * still being parsed, before any of this code runs, and the whole page is lost. Making
 * that survivable would mean decoding every entry separately through a custom
 * serializer, which is a lot of machinery for something a working TMDB never sends. The
 * repository treats it as a failed request instead. [tmdbJson] documents the parser side
 * of this.
 */

private const val DIRECTOR_JOB = "Director"

/** The design shows the first five actors on Movie Details. */
private const val MAX_CAST_MEMBERS = 5

fun MoviePageDto.toDomainMovies(): List<Movie> {
  val movies = mutableListOf<Movie>()
  for (summary in results) {
    val movie = summary.toDomainOrNull()
    if (movie != null) {
      movies.add(movie)
    }
  }
  return movies
}

/**
 * Returns null when the entry cannot be shown as a card. A movie without a title would
 * be an unlabelled poster the user cannot make sense of, and one without an id cannot
 * be opened or saved, so either is left out of the list.
 */
fun MovieSummaryDto.toDomainOrNull(): Movie? {
  val usableId = usableIdOrNull(id) ?: return null
  val usableTitle = textOrNull(title) ?: return null
  return Movie(
    id = usableId,
    title = usableTitle,
    posterPath = imagePathOrNull(posterPath),
    releaseYear = releaseYearOf(releaseDate),
    genreIds = genreIds.filter { it > 0 }.toSet(),
    rating = ratingOf(voteAverage, voteCount),
    voteCount = voteCount?.takeIf { it >= 0 },
    releaseDate = releaseDateOf(releaseDate)
  )
}

/**
 * Returns null when the response has no id or no title, for the same reason as above.
 *
 * The caller reaches this screen from a card that already had a title and a poster, so
 * a null here does not have to become an error screen: showing what the list already
 * knew is a reasonable fallback. That decision belongs to the screen, not here.
 *
 * [credits] defaults to the credits appended to the response. Pass them explicitly when
 * the API service fetches credits with a separate request.
 */
fun MovieDetailsDto.toDomainOrNull(credits: CreditsDto? = this.credits): MovieDetails? {
  val usableId = usableIdOrNull(id) ?: return null
  val usableTitle = textOrNull(title) ?: return null
  return MovieDetails(
    id = usableId,
    title = usableTitle,
    releaseYear = releaseYearOf(releaseDate),
    runtimeMinutes = runtimeMinutesOf(runtime),
    rating = ratingOf(voteAverage, voteCount),
    genres = genres.toDomainGenres(),
    director = directorOf(credits),
    overview = textOrNull(overview),
    posterPath = imagePathOrNull(posterPath),
    backdropPath = imagePathOrNull(backdropPath),
    topCast = topCastOf(credits)
  )
}

fun List<GenreDto>.toDomainGenres(): List<Genre> {
  val genres = mutableListOf<Genre>()
  for (genreDto in this) {
    val genre = genreDto.toDomainOrNull()
    if (genre != null) {
      genres.add(genre)
    }
  }
  return genres
}

/** A genre chip with no usable id or no label is dropped rather than drawn empty. */
fun GenreDto.toDomainOrNull(): Genre? {
  val usableId = usableIdOrNull(id) ?: return null
  val usableName = textOrNull(name) ?: return null
  return Genre(id = usableId, name = usableName)
}

/**
 * TMDB identifiers are positive. A zero or negative one is not something we can request
 * again or save, so it counts as missing rather than as a usable entry.
 */
private fun usableIdOrNull(id: Int?): Int? {
  if (id == null || id <= 0) {
    return null
  }
  return id
}

/**
 * TMDB release dates look like "2024-03-15", but the field is an empty string for
 * movies without a confirmed date and is missing entirely for some entries. Anything
 * that does not start with a readable year counts as an unknown year.
 */
private fun releaseYearOf(releaseDate: String?): Int? {
  if (releaseDate == null || releaseDate.length < 4) {
    return null
  }
  return releaseDate.substring(0, 4).toIntOrNull()
}

/** A complete calendar date is required when excluding unreleased movies. */
private fun releaseDateOf(value: String?): LocalDate? = try {
  value?.let(LocalDate::parse)
} catch (_: DateTimeParseException) {
  null
}

/**
 * TMDB sends 0 rather than omitting the runtime when it does not know it. The design
 * says a missing runtime is not shown at all, so 0 has to become "unknown" here.
 */
private fun runtimeMinutesOf(runtime: Int?): Int? {
  if (runtime == null || runtime <= 0) {
    return null
  }
  return runtime
}

/**
 * A movie nobody has voted on comes back with an average of 0.0. Shown on a card that
 * reads as a terrible rating rather than as no rating at all, so it becomes null and
 * the screen leaves the rating out.
 */
private fun ratingOf(voteAverage: Double?, voteCount: Int?): Double? {
  if (voteAverage == null || !voteAverage.isFinite() || voteAverage <= 0.0 || voteAverage > 10.0) {
    return null
  }
  if (voteCount != null && voteCount <= 0) {
    return null
  }
  return voteAverage
}

/**
 * The first crew member credited as director. A film with co-directors shows only the
 * first; the design has one director line.
 */
private fun directorOf(credits: CreditsDto?): String? {
  if (credits == null) {
    return null
  }
  for (crewMember in credits.crew) {
    if (crewMember.job == DIRECTOR_JOB) {
      val name = textOrNull(crewMember.name)
      if (name != null) {
        return name
      }
    }
  }
  return null
}

/**
 * The first few actors in billing order.
 *
 * TMDB credits an actor once per character, so somebody playing three roles takes three
 * of the five slots and the screen would read "Peter Sellers, Peter Sellers, Peter
 * Sellers, ...". Each actor is therefore counted once.
 */
private fun topCastOf(credits: CreditsDto?): List<String> {
  if (credits == null) {
    return emptyList()
  }
  // TMDB usually returns the cast in billing order already, but sorting by it here
  // means the screen does not depend on that. Entries without an order go last.
  val byBillingOrder = credits.cast.sortedBy { castMember -> castMember.order ?: Int.MAX_VALUE }

  val names = mutableListOf<String>()
  val alreadyAdded = mutableSetOf<String>()
  for (castMember in byBillingOrder) {
    if (names.size == MAX_CAST_MEMBERS) {
      break
    }
    val name = textOrNull(castMember.name) ?: continue

    // Prefer the person id, because two different actors can share a name. Fall back to
    // the name when TMDB did not send an id.
    val identity = if (castMember.id != null) "id:" + castMember.id else "name:" + name
    if (alreadyAdded.add(identity)) {
      names.add(name)
    }
  }
  return names
}

/** TMDB often returns an empty string instead of omitting a text field. */
private fun textOrNull(value: String?): String? {
  if (value == null || value.isBlank()) {
    return null
  }
  return value.trim()
}

/**
 * Image paths are text too, and an empty one would be appended to the TMDB image host
 * and requested as if it were a picture. A missing poster has to stay null so the
 * screen can draw the placeholder the design asks for.
 */
private fun imagePathOrNull(path: String?): String? {
  return textOrNull(path)
}
