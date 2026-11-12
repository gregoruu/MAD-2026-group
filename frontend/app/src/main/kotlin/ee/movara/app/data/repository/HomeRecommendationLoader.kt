package ee.movara.app.data.repository

import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieDiscovery
import ee.movara.app.domain.model.MoviePage
import ee.movara.app.domain.model.MovieSort
import ee.movara.app.domain.model.MovieTastePreferences
import ee.movara.app.domain.recommendation.RecommendationBatch
import ee.movara.app.domain.recommendation.RecommendationSource
import ee.movara.app.domain.recommendation.RecommendationSeeds
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit


data class HomeRecommendationResult(
  val batches: List<RecommendationBatch>,
  val failedSources: Set<RecommendationSource>,
  val staleSources: Set<RecommendationSource>
) {
  val hasFailures: Boolean get() = failedSources.isNotEmpty()
  val usingCache: Boolean get() = staleSources.isNotEmpty()
}


/** Fetch a bounded first page per source, sharing MovieRepository's validation and offline cache. */
class HomeRecommendationLoader(private val repository: MovieRepository) {
  suspend fun load(
    preferences: MovieTastePreferences,
    likedMovies: List<Movie>,
    today: LocalDate,
    forceRefresh: Boolean = false,
    retryFailedFrom: HomeRecommendationResult? = null
  ): HomeRecommendationResult = coroutineScope {
    val seeds = linkedMapOf<Int, RecommendationSource.Seed>()
    // Room returns newest Likes first. Quiz favourites have no timestamp, so use stable ID order.
    RecommendationSeeds.selectLikes(likedMovies, today).forEach {
      seeds[it.id] = RecommendationSource.Seed(it.id, it.title)
    }
    preferences.favouriteMovieIds.sorted().take(5).forEach { id ->
      seeds.putIfAbsent(id, RecommendationSource.Seed(id, preferences.favouriteTitles[id]))
    }
    val sources = seeds.values +
            preferences.genreIds.sorted().map(RecommendationSource::Genre) +
            RecommendationSource.Discovery

    // Keep successful batches when retrying failed sources.
    val previousBatches = retryFailedFrom
      ?.batches
      .orEmpty()
      .associateBy { it.source }

    val sourcesToFetch = sources.filter { source ->
      retryFailedFrom == null ||
              source in retryFailedFrom.failedSources ||
              source !in previousBatches
    }

    val semaphore = Semaphore(3)

    val fetched = sourcesToFetch.map { source ->
      async {
        semaphore.withPermit {
          source to fetch(
            source,
            preferences,
            today,
            if (forceRefresh) {
              MovieRequestPolicy.NETWORK_FIRST
            } else {
              MovieRequestPolicy.PREFER_FRESH_CACHE
            }
          )
        }
      }
    }.awaitAll().toMap()

    // Preserve original source order and successful results.
    val batches = sources.mapNotNull { source ->
      when (val result = fetched[source]) {
        is RepositoryResult.Success ->
          RecommendationBatch(source, result.data.movies)

        is RepositoryResult.Error -> null

        null -> previousBatches[source]
      }
    }

    val failedSources = fetched
      .filterValues { it is RepositoryResult.Error }
      .keys

    val staleSources = retryFailedFrom
      ?.staleSources
      .orEmpty()
      .intersect(sources.toSet())
      .toMutableSet()

    for ((source, result) in fetched) {
      if (
        result is RepositoryResult.Success &&
        result.source == DataSource.CACHE &&
        !result.freshCacheHit
      ) {
        staleSources.add(source)
      } else {
        staleSources.remove(source)
      }
    }

    HomeRecommendationResult(
      batches = batches,
      failedSources = failedSources,
      staleSources = staleSources
    )
  }

    private suspend fun fetch(
    source: RecommendationSource,
    preferences: MovieTastePreferences,
    today: LocalDate,
    policy: MovieRequestPolicy
  ): RepositoryResult<MoviePage> = try {
    when (source) {
      is RecommendationSource.Seed -> repository.recommendedMovies(source.movieId, policy = policy)
      is RecommendationSource.Genre -> repository.discoverMovies(
        MovieDiscovery.familiar(source.genreId, today).copy(
          sort = MovieSort.POPULARITY,
          minimumRating = preferences.minimumRating
        ),
        policy = policy
      )
      RecommendationSource.Discovery -> repository.trendingMovies(policy = policy)
    }
  } catch (cancelled: CancellationException) {
    throw cancelled
  } catch (_: Exception) {
    RepositoryResult.Error(ErrorCategory.UNEXPECTED, "Could not load recommendations.")
  }
}
