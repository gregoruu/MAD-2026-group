package ee.movara.app.ui.preferences

import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.DataSource
import ee.movara.app.data.repository.ErrorCategory
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.MovieTasteRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieDetails
import ee.movara.app.domain.model.MovieDiscovery
import java.time.LocalDate
import ee.movara.app.domain.model.MoviePage
import ee.movara.app.domain.model.MovieTastePreferences
import ee.movara.app.domain.model.MovieTasteProfile
import ee.movara.app.domain.model.OnboardingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class QuizTasteRepository : MovieTasteRepository {
  val stored = MutableStateFlow(MovieTasteProfile())
  var readFailure: Exception? = null
  var writeFailure: Exception? = null
  var saveCalls = 0
  var skipCalls = 0
  var saveDelay = 0L

  override val profile = flow {
    readFailure?.let { throw it }
    emitAll(stored)
  }

  override suspend fun savePreferences(preferences: MovieTastePreferences) {
    saveCalls++
    delay(saveDelay)
    writeFailure?.let { throw it }
    stored.value = MovieTasteProfile(preferences, OnboardingStatus.COMPLETED)
  }

  override suspend fun skipOnboarding() {
    skipCalls++
    writeFailure?.let { throw it }
    if (stored.value.needsOnboarding) {
      stored.value = stored.value.copy(onboardingStatus = OnboardingStatus.SKIPPED)
    }
  }
}

internal fun quizMovie(id: Int, title: String = "Movie $id") = Movie(
  id, title, null, 2020, rating = 7.5, voteCount = 1000,
  releaseDate = LocalDate.of(2020, 1, 1)
)

internal fun quizPage(
  movies: List<Movie>,
  page: Int = 1,
  totalPages: Int = 2
): RepositoryResult<MoviePage> = RepositoryResult.Success(
  MoviePage(movies, page, totalPages, totalPages * 20), DataSource.NETWORK
)

internal class QuizMovieRepository : MovieRepository {
  val requests = mutableListOf<Pair<String, Int>>()
  val discoveryRequests = mutableListOf<Pair<MovieDiscovery, Int>>()
  val detailsRequests = mutableListOf<Int>()
  var respond: suspend (String, Int) -> RepositoryResult<MoviePage> = { source, page ->
    quizPage(listOf(quizMovie(page, source)), page)
  }
  private suspend fun request(source: String, page: Int): RepositoryResult<MoviePage> {
    requests += source to page
    return respond(source, page)
  }

  override suspend fun discoverMovies(options: MovieDiscovery, page: Int): RepositoryResult<MoviePage> {
    discoveryRequests += options to page
    return request("discover:${options.genreId ?: "all"}", page)
  }
  override suspend fun searchMovies(title: String, page: Int) = request("search:$title", page)
  override suspend fun trendingMovies(page: Int) = request("trending", page)
  override suspend fun moviesInGenre(genreId: Int, page: Int) = request("genre:$genreId", page)
  override suspend fun movieDetails(movieId: Int): RepositoryResult<MovieDetails> {
    detailsRequests += movieId
    return RepositoryResult.Error(ErrorCategory.NETWORK, "Details unavailable")
  }
  override suspend fun recommendedMovies(movieId: Int, page: Int): RepositoryResult<MoviePage> =
    error("Quiz must not request recommendations")
  override fun observeMovies(listType: MovieListType): Flow<RepositoryResult<List<Movie>>> =
    error("Quiz must not access saved movie lists")
  override fun observeCount(listType: MovieListType): Flow<RepositoryResult<Int>> = error("Unused")
  override fun observeMembership(movieId: Int, listType: MovieListType): Flow<RepositoryResult<Boolean>> =
    error("Unused")
  override suspend fun saveMovie(movie: Movie, listType: MovieListType): RepositoryResult<Unit> =
    error("Quiz must not change saved movie lists")
  override suspend fun removeMovie(movieId: Int, listType: MovieListType): RepositoryResult<Unit> =
    error("Quiz must not change saved movie lists")
}
