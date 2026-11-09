package ee.movara.app.domain.recommendation

import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieTastePreferences
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class MovieRecommendationsTest {
  private val today = LocalDate.of(2026, 11, 9)
  private val seed = RecommendationSource.Seed(1, "Favourite")
  private fun movie(id: Int, rating: Double? = 7.0, votes: Int = 1000, genre: Int = 28) =
    Movie(id, "Movie $id", null, 2020, setOf(genre), rating, votes, today.minusYears(1))

  private fun rank(
    vararg batches: RecommendationBatch,
    preferences: MovieTastePreferences = MovieTastePreferences(),
    liked: Set<Int> = emptySet(),
    watched: Set<Int> = emptySet()
  ) = MovieRecommendations.rank(batches.toList(), preferences, liked, watched, today)

  @Test fun overlapThenGenreMatchesThenReliableRatingAndVoteCountDetermineRanking() {
    val candidates = listOf(
      movie(10, 9.0), movie(20, 6.0), movie(30, 8.0), movie(40, 9.9, votes = 2),
      movie(50, 7.0, votes = 2000), movie(60, 7.0, votes = 1000),
      movie(70, 10.0, genre = 18)
    )
    val ranked = rank(
      RecommendationBatch(seed, candidates),
      RecommendationBatch(RecommendationSource.Seed(2, "Other favourite"), listOf(candidates[1])),
      preferences = MovieTastePreferences(genreIds = setOf(28))
    )
    assertEquals(listOf(20, 10, 30, 50, 60, 40, 70), ranked.map { it.movie.id })
    assertEquals(2, ranked.first().seedCount)
  }

  @Test fun duplicateMoviesAndRepeatedSourcesDoNotInflateOverlap() {
    val batch = RecommendationBatch(seed, listOf(movie(10), movie(10)))
    val ranked = rank(batch, batch, RecommendationBatch(RecommendationSource.Discovery, listOf(movie(10))))
    assertEquals(1, ranked.size)
    assertEquals(1, ranked.single().seedCount)
    assertEquals(2, ranked.single().sources.size)
  }

  @Test fun watchedLikesAndAllQuizFavouritesAreExcludedEvenWhenNotFetchedAsSeeds() {
    val ranked = rank(
      RecommendationBatch(seed, (1..9).map { movie(it) }),
      preferences = MovieTastePreferences(favouriteMovieIds = setOf(1, 2, 3, 4, 5, 6)),
      liked = setOf(7),
      watched = setOf(8)
    )
    assertEquals(listOf(9), ranked.map { it.movie.id })
  }

  @Test fun minimumRatingAppliesToEverySourceIncludingUnknownRatingsAndDiscovery() {
    val preferences = MovieTastePreferences(minimumRating = 8)
    val sources = listOf(seed, RecommendationSource.Genre(28), RecommendationSource.Discovery)
    for (source in sources) {
      val ranked = rank(
        RecommendationBatch(source, listOf(movie(10, 7.9), movie(20, null), movie(30, 8.0))),
        preferences = preferences
      )
      assertEquals(listOf(30), ranked.map { it.movie.id })
      assertTrue(rank(RecommendationBatch(source, listOf(movie(10, 7.9))),
        preferences = preferences).isEmpty())
    }
  }

  @Test fun noMinimumAllowsUnknownRatingsButFutureFilmsAreStillExcluded() {
    val ranked = rank(RecommendationBatch(RecommendationSource.Discovery, listOf(
      movie(10, null).copy(releaseDate = null),
      movie(20).copy(releaseDate = today.plusDays(1)),
      movie(30).copy(releaseDate = today)
    )))
    assertEquals(setOf(10, 30), ranked.map { it.movie.id }.toSet())
  }

  @Test fun presentationAlternatesSourcesAndGenresWithoutChangingTheTopPick() {
    val ranked = rank(
      RecommendationBatch(seed, listOf(movie(10, 9.0), movie(20, 8.9))),
      RecommendationBatch(RecommendationSource.Seed(2, "Comedy favourite"),
        listOf(movie(30, 7.5, genre = 35)))
    )
    assertEquals(listOf(10, 30, 20), ranked.map { it.movie.id })
  }

  @Test fun discoveryIsASmallerSelectionWhenPersonalizedCandidatesExist() {
    val ranked = rank(
      RecommendationBatch(seed, listOf(movie(10))),
      RecommendationBatch(RecommendationSource.Discovery, (100..130).map { movie(it) })
    )
    assertEquals(10, ranked.first().movie.id)
    assertEquals(4, ranked.count { it.isDiscovery })
    assertEquals(5, ranked.size)
  }

  @Test fun skippedQuizCanFillTheFeedWithDiscoveryAndTiesAreStable() {
    val ranked = rank(RecommendationBatch(RecommendationSource.Discovery,
      (100..130).reversed().map { movie(it) }))
    assertEquals(24, ranked.size)
    assertEquals((100..123).toList(), ranked.map { it.movie.id })
    assertTrue(ranked.all { it.isDiscovery })
  }

  @Test fun genreSourceRetainsAReasonWhenMovieMetadataIsSparse() {
    val ranked = rank(
      RecommendationBatch(RecommendationSource.Genre(28), listOf(movie(10).copy(genreIds = emptySet()))),
      preferences = MovieTastePreferences(genreIds = setOf(28))
    )
    assertEquals(setOf(28), ranked.single().preferredGenreIds)
    assertFalse(ranked.single().isDiscovery)
  }
}
