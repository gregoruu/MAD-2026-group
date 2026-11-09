package ee.movara.app.data.repository

import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieDiscovery
import ee.movara.app.domain.model.MoviePage
import ee.movara.app.domain.model.MovieSort
import ee.movara.app.domain.model.MovieTastePreferences
import ee.movara.app.domain.recommendation.RecommendationBatch
import ee.movara.app.domain.recommendation.RecommendationSource
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class HomeRecommendationResult(
  val batches: List<RecommendationBatch>,
  val hasFailures: Boolean,
  val usingCache: Boolean
)

/** Fetch a bounded first page per source, sharing MovieRepository's validation and offline cache. */
class HomeRecommendationLoader(private val repository: MovieRepository) {
  suspend fun load(
    preferences: MovieTastePreferences,
    likedMovies: List<Movie>,
    today: LocalDate
  ): HomeRecommendationResult = coroutineScope {
    val seeds = linkedMapOf<Int, RecommendationSource.Seed>()
    // Room returns newest Likes first. Quiz favourites have no timestamp, so use stable ID order.
    likedMovies.take(5).forEach { seeds[it.id] = RecommendationSource.Seed(it.id, it.title) }
    preferences.favouriteMovieIds.sorted().take(5).forEach { id ->
      seeds.putIfAbsent(id, RecommendationSource.Seed(id, preferences.favouriteTitles[id]))
    }
    val sources = seeds.values +
      preferences.genreIds.sorted().map(RecommendationSource::Genre) +
      RecommendationSource.Discovery
    val semaphore = Semaphore(3)
    val results = sources.map { source ->
      async {
        semaphore.withPermit {
          source to fetch(source, preferences, today)
        }
      }
    }.awaitAll()
    HomeRecommendationResult(
      batches = results.mapNotNull { (source, result) ->
        (result as? RepositoryResult.Success)?.let { RecommendationBatch(source, it.data.movies) }
      },
      hasFailures = results.any { it.second is RepositoryResult.Error },
      usingCache = results.any { (_, result) ->
        result is RepositoryResult.Success && result.source == DataSource.CACHE
      }
    )
  }

  private suspend fun fetch(
    source: RecommendationSource,
    preferences: MovieTastePreferences,
    today: LocalDate
  ): RepositoryResult<MoviePage> = try {
    when (source) {
      is RecommendationSource.Seed -> repository.recommendedMovies(source.movieId)
      is RecommendationSource.Genre -> repository.discoverMovies(
        MovieDiscovery.familiar(source.genreId, today).copy(
          sort = MovieSort.POPULARITY,
          minimumRating = preferences.minimumRating
        )
      )
      RecommendationSource.Discovery -> repository.trendingMovies()
    }
  } catch (cancelled: CancellationException) {
    throw cancelled
  } catch (_: Exception) {
    RepositoryResult.Error(ErrorCategory.UNEXPECTED, "Could not load recommendations.")
  }
}
