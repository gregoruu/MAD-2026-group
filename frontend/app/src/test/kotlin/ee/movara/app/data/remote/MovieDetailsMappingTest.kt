package ee.movara.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the single movie response and the credits that go with it. The rules here
 * follow the "Data and content rules" section of docs/design-specs.md: a missing
 * runtime or cast is not shown, and a missing description is handled by the screen.
 */
class MovieDetailsMappingTest {

  @Test
  fun toDomainOrNull_mapsAFullDetailsResponse() {
    val json = """
      {
        "adult": false,
        "backdrop_path": "/backdrop.jpg",
        "budget": 63000000,
        "genres": [ { "id": 28, "name": "Action" }, { "id": 878, "name": "Science Fiction" } ],
        "id": 603,
        "overview": "A computer hacker learns the truth.",
        "poster_path": "/matrix.jpg",
        "release_date": "1999-03-30",
        "runtime": 136,
        "status": "Released",
        "title": "The Matrix",
        "vote_average": 8.2,
        "vote_count": 25000,
        "credits": {
          "cast": [
            { "id": 6384, "name": "Keanu Reeves", "order": 0 },
            { "id": 2975, "name": "Laurence Fishburne", "order": 1 }
          ],
          "crew": [
            { "id": 1, "name": "Editor Person", "job": "Editor" },
            { "id": 9339, "name": "Lana Wachowski", "job": "Director" }
          ]
        }
      }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertEquals(603, details.id)
    assertEquals("The Matrix", details.title)
    assertEquals(1999, details.releaseYear)
    assertEquals(136, details.runtimeMinutes)
    assertEquals(8.2, details.rating!!, 0.001)
    assertEquals(listOf("Action", "Science Fiction"), details.genres.map { it.name })
    assertEquals("Lana Wachowski", details.director)
    assertEquals("A computer hacker learns the truth.", details.overview)
    assertEquals("/matrix.jpg", details.posterPath)
    assertEquals("/backdrop.jpg", details.backdropPath)
    assertEquals(listOf("Keanu Reeves", "Laurence Fishburne"), details.topCast)
  }

  @Test
  fun toDomainOrNull_mapsAResponseWithOnlyAnIdAndTitle() {
    val details = tmdbJson
      .decodeFromString(MovieDetailsDto.serializer(), """{ "id": 7, "title": "Obscure Film" }""")
      .toDomainOrNull()!!

    assertEquals("Obscure Film", details.title)
    assertNull(details.releaseYear)
    assertNull(details.runtimeMinutes)
    assertNull(details.rating)
    assertNull(details.director)
    assertNull(details.overview)
    assertNull(details.posterPath)
    assertNull(details.backdropPath)
    assertTrue(details.genres.isEmpty())
    assertTrue(details.topCast.isEmpty())
  }

  @Test
  fun toDomainOrNull_returnsNullWhenThereIsNoTitle() {
    val details = tmdbJson
      .decodeFromString(MovieDetailsDto.serializer(), """{ "id": 7 }""")
      .toDomainOrNull()

    assertNull(details)
  }

  @Test
  fun toDomainOrNull_treatsAZeroRuntimeAsUnknown() {
    val details = tmdbJson
      .decodeFromString(MovieDetailsDto.serializer(), """{ "id": 7, "title": "Film", "runtime": 0 }""")
      .toDomainOrNull()!!

    assertNull(details.runtimeMinutes)
  }

  @Test
  fun toDomainOrNull_treatsNoVotesAsNoRating() {
    val json = """
      { "id": 7, "title": "Film", "vote_average": 0.0, "vote_count": 0 }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertNull(details.rating)
  }

  @Test
  fun toDomainOrNull_treatsAnAverageWithoutVotesAsNoRating() {
    val json = """
      { "id": 7, "title": "Film", "vote_average": 7.5, "vote_count": 0 }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertNull(details.rating)
  }

  @Test
  fun toDomainOrNull_leavesOutAGenreWithoutAName() {
    val json = """
      {
        "id": 7,
        "title": "Film",
        "genres": [ { "id": 28, "name": "Action" }, { "id": 99 }, { "id": 12, "name": "" } ]
      }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertEquals(listOf("Action"), details.genres.map { it.name })
  }

  @Test
  fun toDomainOrNull_keepsAtMostFiveActors() {
    val cast = (0..9).joinToString(", ") { index ->
      """{ "id": $index, "name": "Actor $index", "order": $index }"""
    }
    val json = """{ "id": 7, "title": "Film", "credits": { "cast": [ $cast ] } }"""

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertEquals(listOf("Actor 0", "Actor 1", "Actor 2", "Actor 3", "Actor 4"), details.topCast)
  }

  @Test
  fun toDomainOrNull_sortsActorsByBillingOrder() {
    val json = """
      {
        "id": 7,
        "title": "Film",
        "credits": {
          "cast": [
            { "name": "Third", "order": 2 },
            { "name": "First", "order": 0 },
            { "name": "Unordered" },
            { "name": "Second", "order": 1 }
          ]
        }
      }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertEquals(listOf("First", "Second", "Third", "Unordered"), details.topCast)
  }

  @Test
  fun toDomainOrNull_leavesOutAnActorWithoutAName() {
    val json = """
      {
        "id": 7,
        "title": "Film",
        "credits": { "cast": [ { "order": 0 }, { "name": "Named Actor", "order": 1 } ] }
      }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertEquals(listOf("Named Actor"), details.topCast)
  }

  @Test
  fun toDomainOrNull_hasNoDirectorWhenNobodyIsCreditedAsOne() {
    val json = """
      {
        "id": 7,
        "title": "Film",
        "credits": { "crew": [ { "name": "Someone", "job": "Producer" } ] }
      }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertNull(details.director)
  }

  @Test
  fun toDomainOrNull_usesCreditsFetchedSeparately() {
    val details = tmdbJson
      .decodeFromString(MovieDetailsDto.serializer(), """{ "id": 7, "title": "Film" }""")
      .toDomainOrNull(
        credits = CreditsDto(
          cast = listOf(CastMemberDto(name = "Lead Actor", order = 0)),
          crew = listOf(CrewMemberDto(name = "Some Director", job = "Director"))
        )
      )!!

    assertEquals("Some Director", details.director)
    assertEquals(listOf("Lead Actor"), details.topCast)
  }

  @Test
  fun toDomainOrNull_returnsNullWhenThereIsNoId() {
    val details = tmdbJson
      .decodeFromString(MovieDetailsDto.serializer(), """{ "title": "Film" }""")
      .toDomainOrNull()

    assertNull(details)
  }

  @Test
  fun toDomainOrNull_treatsEmptyImagePathsAsMissingImages() {
    val json = """
      { "id": 7, "title": "Film", "poster_path": "", "backdrop_path": "" }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertNull(details.posterPath)
    assertNull(details.backdropPath)
  }

  @Test
  fun toDomainOrNull_countsAnActorWithSeveralRolesOnce() {
    val json = """
      {
        "id": 7,
        "title": "Film",
        "credits": {
          "cast": [
            { "id": 1, "name": "Peter Sellers", "order": 0 },
            { "id": 1, "name": "Peter Sellers", "order": 1 },
            { "id": 1, "name": "Peter Sellers", "order": 2 },
            { "id": 2, "name": "George C. Scott", "order": 3 },
            { "id": 3, "name": "Sterling Hayden", "order": 4 },
            { "id": 4, "name": "Keenan Wynn", "order": 5 },
            { "id": 5, "name": "Slim Pickens", "order": 6 },
            { "id": 6, "name": "Peter Bull", "order": 7 }
          ]
        }
      }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertEquals(
      listOf(
        "Peter Sellers",
        "George C. Scott",
        "Sterling Hayden",
        "Keenan Wynn",
        "Slim Pickens"
      ),
      details.topCast
    )
  }

  @Test
  fun toDomainOrNull_keepsTwoDifferentActorsWhoShareAName() {
    val json = """
      {
        "id": 7,
        "title": "Film",
        "credits": {
          "cast": [
            { "id": 1, "name": "Chris Evans", "order": 0 },
            { "id": 2, "name": "Chris Evans", "order": 1 }
          ]
        }
      }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertEquals(listOf("Chris Evans", "Chris Evans"), details.topCast)
  }

  @Test
  fun toDomainOrNull_leavesOutAGenreWithoutAnId() {
    val json = """
      {
        "id": 7,
        "title": "Film",
        "genres": [ { "name": "No Id Genre" }, { "id": 28, "name": "Action" } ]
      }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertEquals(listOf("Action"), details.genres.map { it.name })
  }

  @Test
  fun toDomainOrNull_returnsNullWhenTheIdIsNotPositive() {
    val zero = tmdbJson
      .decodeFromString(MovieDetailsDto.serializer(), """{ "id": 0, "title": "Film" }""")
      .toDomainOrNull()
    val negative = tmdbJson
      .decodeFromString(MovieDetailsDto.serializer(), """{ "id": -5, "title": "Film" }""")
      .toDomainOrNull()

    assertNull(zero)
    assertNull(negative)
  }

  @Test
  fun toDomainOrNull_leavesOutAGenreWhoseIdIsNotPositive() {
    val json = """
      {
        "id": 7,
        "title": "Film",
        "genres": [ { "id": 0, "name": "Zero" }, { "id": 28, "name": "Action" } ]
      }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertEquals(listOf("Action"), details.genres.map { it.name })
  }

  @Test
  fun toDomainOrNull_treatsExplicitlyNullListsAsEmpty() {
    val json = """
      {
        "id": 7,
        "title": "Film",
        "genres": null,
        "credits": { "cast": null, "crew": null }
      }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertTrue(details.genres.isEmpty())
    assertTrue(details.topCast.isEmpty())
    assertNull(details.director)
  }

  @Test
  fun toDomainOrNull_handlesNullEntriesInsideCastAndCrewFields() {
    val json = """
      {
        "id": 7,
        "title": "Film",
        "credits": {
          "cast": [ { "id": null, "name": null, "order": null }, { "name": "Real Actor" } ],
          "crew": [ { "name": null, "job": "Director" }, { "name": "Real Director", "job": "Director" } ]
        }
      }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertEquals(listOf("Real Actor"), details.topCast)
    assertEquals("Real Director", details.director)
  }

  @Test
  fun toDomainOrNull_handlesANullGenreName() {
    val json = """
      { "id": 7, "title": "Film", "genres": [ { "id": 28, "name": null } ] }
    """.trimIndent()

    val details = tmdbJson.decodeFromString(MovieDetailsDto.serializer(), json).toDomainOrNull()!!

    assertTrue(details.genres.isEmpty())
  }

  @Test
  fun decode_parsesCreditsWithNoCastOrCrew() {
    val credits = tmdbJson.decodeFromString(CreditsDto.serializer(), """{ "id": 7 }""")

    assertTrue(credits.cast.isEmpty())
    assertTrue(credits.crew.isEmpty())
  }
}
