package ee.movara.app.data.remote

import java.time.LocalDate
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the response shape shared by search, trending, recommendations and
 * discover-by-genre. The JSON in these tests is written the way TMDB sends it,
 * including fields the app does not read, so the tests exercise [tmdbJson] too.
 */
class MoviePageMappingTest {

  @Test
  fun toDomainMovies_mapsAFullSearchResponse() {
    val json = """
      {
        "page": 1,
        "results": [
          {
            "adult": false,
            "backdrop_path": "/backdrop.jpg",
            "genre_ids": [28, 878],
            "id": 603,
            "original_language": "en",
            "overview": "A computer hacker learns the truth.",
            "popularity": 112.4,
            "poster_path": "/matrix.jpg",
            "release_date": "1999-03-30",
            "title": "The Matrix",
            "video": false,
            "vote_average": 8.2,
            "vote_count": 25000
          }
        ],
        "total_pages": 1,
        "total_results": 1
      }
    """.trimIndent()

    val movies = tmdbJson.decodeFromString(MoviePageDto.serializer(), json).toDomainMovies()

    assertEquals(1, movies.size)
    assertEquals(603, movies[0].id)
    assertEquals("The Matrix", movies[0].title)
    assertEquals("/matrix.jpg", movies[0].posterPath)
    assertEquals(1999, movies[0].releaseYear)
    assertEquals(LocalDate.of(1999, 3, 30), movies[0].releaseDate)
    assertEquals(setOf(28, 878), movies[0].genreIds)
    assertEquals(8.2, movies[0].rating!!, 0.001)
    assertEquals(25000, movies[0].voteCount)
  }

  @Test
  fun toDomainMovies_mapsAResultWithOnlyAnIdAndTitle() {
    val json = """
      { "results": [ { "id": 7, "title": "Obscure Film" } ] }
    """.trimIndent()

    val movies = tmdbJson.decodeFromString(MoviePageDto.serializer(), json).toDomainMovies()

    assertEquals(1, movies.size)
    assertEquals("Obscure Film", movies[0].title)
    assertNull(movies[0].posterPath)
    assertNull(movies[0].releaseYear)
  }

  @Test
  fun toDomainMovies_treatsExplicitNullsAsMissingValues() {
    val json = """
      {
        "results": [
          { "id": 7, "title": "Obscure Film", "poster_path": null, "release_date": null }
        ]
      }
    """.trimIndent()

    val movies = tmdbJson.decodeFromString(MoviePageDto.serializer(), json).toDomainMovies()

    assertEquals(1, movies.size)
    assertNull(movies[0].posterPath)
    assertNull(movies[0].releaseYear)
  }

  @Test
  fun toDomainMovies_treatsAnEmptyReleaseDateAsAnUnknownYear() {
    val json = """
      { "results": [ { "id": 7, "title": "Unreleased", "release_date": "" } ] }
    """.trimIndent()

    val movies = tmdbJson.decodeFromString(MoviePageDto.serializer(), json).toDomainMovies()

    assertNull(movies[0].releaseYear)
  }

  @Test
  fun toDomainMovies_treatsAnUnreadableReleaseDateAsAnUnknownYear() {
    val json = """
      { "results": [ { "id": 7, "title": "Odd Date", "release_date": "soon" } ] }
    """.trimIndent()

    val movies = tmdbJson.decodeFromString(MoviePageDto.serializer(), json).toDomainMovies()

    assertNull(movies[0].releaseYear)
  }

  @Test
  fun discoveryMetadataHandlesMissingAndInvalidValues() {
    val sparse = MovieSummaryDto(id = 1, title = "Sparse").toDomainOrNull()!!
    assertTrue(sparse.genreIds.isEmpty())
    assertNull(sparse.rating)
    assertNull(sparse.voteCount)
    assertNull(sparse.releaseDate)

    val invalid = MovieSummaryDto(
      id = 2, title = "Invalid metadata", releaseDate = "2026-02-30",
      genreIds = listOf(28, 28, 0, -1), voteAverage = 11.0, voteCount = -1
    ).toDomainOrNull()!!
    assertEquals(setOf(28), invalid.genreIds)
    assertNull(invalid.rating)
    assertNull(invalid.voteCount)
    assertNull(invalid.releaseDate)
    assertNull(MovieSummaryDto(
      id = 3, title = "No votes", voteAverage = 9.0, voteCount = 0
    ).toDomainOrNull()!!.rating)
  }

  @Test
  fun toDomainMovies_leavesOutAResultWithoutATitle() {
    val json = """
      {
        "results": [
          { "id": 1, "title": "Shown" },
          { "id": 2 },
          { "id": 3, "title": "   " },
          { "id": 4, "title": null }
        ]
      }
    """.trimIndent()

    val movies = tmdbJson.decodeFromString(MoviePageDto.serializer(), json).toDomainMovies()

    assertEquals(1, movies.size)
    assertEquals(1, movies[0].id)
  }

  @Test
  fun toDomainMovies_treatsAnEmptyPosterPathAsNoPoster() {
    val json = """
      { "results": [ { "id": 7, "title": "No Artwork", "poster_path": "" } ] }
    """.trimIndent()

    val movies = tmdbJson.decodeFromString(MoviePageDto.serializer(), json).toDomainMovies()

    assertNull(movies[0].posterPath)
  }

  @Test
  fun toDomainMovies_keepsTheOtherResultsWhenOneEntryHasNoId() {
    val json = """
      {
        "results": [
          { "id": 1, "title": "First" },
          { "title": "Broken entry with no id" },
          { "id": null, "title": "Explicitly null id" },
          { "id": 4, "title": "Last" }
        ]
      }
    """.trimIndent()

    val movies = tmdbJson.decodeFromString(MoviePageDto.serializer(), json).toDomainMovies()

    assertEquals(listOf(1, 4), movies.map { it.id })
  }

  @Test
  fun toDomainMovies_leavesOutAResultWhoseIdIsNotPositive() {
    val json = """
      {
        "results": [
          { "id": 1, "title": "Usable" },
          { "id": 0, "title": "Zero id" },
          { "id": -5, "title": "Negative id" }
        ]
      }
    """.trimIndent()

    val movies = tmdbJson.decodeFromString(MoviePageDto.serializer(), json).toDomainMovies()

    assertEquals(listOf(1), movies.map { it.id })
  }

  @Test
  fun decode_failsTheWholeResponseWhenAFieldHasTheWrongType() {
    // Documents the one case the drop-the-entry rule does not cover. TMDB does not send
    // responses like this; if it ever did, the request fails rather than losing one row.
    val json = """
      { "results": [ { "id": "invalid", "title": "Film" } ] }
    """.trimIndent()

    assertThrows(SerializationException::class.java) {
      tmdbJson.decodeFromString(MoviePageDto.serializer(), json)
    }
  }

  @Test
  fun decode_parsesAResponseWithNoResultsField() {
    val page = tmdbJson.decodeFromString(MoviePageDto.serializer(), """{ "page": 1 }""")

    assertTrue(page.results.isEmpty())
    assertTrue(page.toDomainMovies().isEmpty())
  }

  @Test
  fun decode_parsesAResponseWhereResultsIsNull() {
    val page = tmdbJson.decodeFromString(MoviePageDto.serializer(), """{ "results": null }""")

    assertTrue(page.results.isEmpty())
  }

  @Test
  fun decode_ignoresFieldsTheAppDoesNotRead() {
    val json = """
      {
        "results": [ { "id": 7, "title": "Film", "a_field_added_later": { "nested": true } } ],
        "some_new_top_level_field": [1, 2, 3]
      }
    """.trimIndent()

    val movies = tmdbJson.decodeFromString(MoviePageDto.serializer(), json).toDomainMovies()

    assertEquals("Film", movies[0].title)
  }

  @Test
  fun decode_keepsPagingInformation() {
    val json = """
      { "page": 2, "results": [], "total_pages": 9, "total_results": 170 }
    """.trimIndent()

    val page = tmdbJson.decodeFromString(MoviePageDto.serializer(), json)

    assertEquals(2, page.page)
    assertEquals(9, page.totalPages)
    assertEquals(170, page.totalResults)
  }
}
