package ee.movara.app.domain.recommendation

import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieTastePreferences
import java.time.LocalDate

sealed interface RecommendationSource {
  data class Seed(val movieId: Int, val title: String?) : RecommendationSource
  data class Genre(val genreId: Int) : RecommendationSource
  data object Discovery : RecommendationSource
}

data class RecommendationBatch(
  val source: RecommendationSource,
  val movies: List<Movie>
)

data class MovieRecommendation(
  val movie: Movie,
  val sources: Set<RecommendationSource> = emptySet(),
  val preferredGenreIds: Set<Int> = emptySet()
) {
  val seedCount: Int get() = sources.filterIsInstance<RecommendationSource.Seed>().size
  val isDiscovery: Boolean get() = seedCount == 0 && preferredGenreIds.isEmpty()
}

/** Deterministic ranking, independent of network, storage and Compose. */
object MovieRecommendations {
  const val MAX_RESULTS = 24
  const val MAX_DISCOVERY = 4

  fun rank(
    batches: List<RecommendationBatch>,
    preferences: MovieTastePreferences,
    likedIds: Set<Int>,
    watchedIds: Set<Int>,
    today: LocalDate
  ): List<MovieRecommendation> {
    val excluded = preferences.favouriteMovieIds + likedIds + watchedIds
    val candidates = linkedMapOf<Int, MovieRecommendation>()
    for (batch in batches) {
      for (movie in batch.movies) {
        if (movie.id in excluded || movie.releaseDate?.isAfter(today) == true) continue
        val minimum = preferences.minimumRating
        // Unknown ratings cannot satisfy a user's explicit minimum.
        if (minimum != null && (movie.rating ?: -1.0) < minimum) continue
        val previous = candidates[movie.id]
        val sources = previous?.sources.orEmpty() + batch.source
        // A discover request for a genre also supplies a match when optional card metadata is sparse.
        val genres = movie.genreIds.intersect(preferences.genreIds) +
          sources.filterIsInstance<RecommendationSource.Genre>()
            .map { it.genreId }.filter { it in preferences.genreIds }
        candidates[movie.id] = MovieRecommendation(
          previous?.movie ?: movie, sources, previous?.preferredGenreIds.orEmpty() + genres
        )
      }
    }
    val ranked = candidates.values.sortedWith(
      compareByDescending<MovieRecommendation> { it.seedCount }
        .thenByDescending { it.preferredGenreIds.size }
        .thenByDescending { (it.movie.voteCount ?: 0) >= 100 }
        .thenByDescending { it.movie.rating ?: 0.0 }
        .thenByDescending { it.movie.voteCount ?: 0 }
        .thenBy { it.movie.id }
    )
    val personalized = ranked.any { !it.isDiscovery }
    var discoveries = 0
    val selected = ranked.filter {
      !personalized || !it.isDiscovery || discoveries++ < MAX_DISCOVERY
    }.take(MAX_RESULTS)

    // Keep the best result first. Then prefer a different source AND genre where available.
    // This changes presentation order only; it never drops eligible personalized candidates.
    val remaining = selected.toMutableList()
    val varied = mutableListOf<MovieRecommendation>()
    while (remaining.isNotEmpty()) {
      val last = varied.lastOrNull()
      val index = if (last == null) 0 else remaining.indexOfFirst {
        sourceKey(it) != sourceKey(last) && genreKey(it) != genreKey(last)
      }.takeIf { it >= 0 } ?: 0
      varied += remaining.removeAt(index)
    }
    return varied
  }

  private fun sourceKey(item: MovieRecommendation): String =
    item.sources.filterIsInstance<RecommendationSource.Seed>().minOfOrNull { it.movieId }
      ?.let { "seed:$it" }
      ?: item.sources.filterIsInstance<RecommendationSource.Genre>().minOfOrNull { it.genreId }
        ?.let { "genre:$it" }
      ?: "discovery"

  private fun genreKey(item: MovieRecommendation): Int? =
    item.preferredGenreIds.minOrNull() ?: item.movie.genreIds.minOrNull()
}
