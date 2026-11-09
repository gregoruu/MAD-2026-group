package ee.movara.app.ui.home

import androidx.lifecycle.viewModelScope
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.DataSource
import ee.movara.app.data.repository.ErrorCategory
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.MovieRequestPolicy
import ee.movara.app.data.repository.MovieTasteRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.*
import ee.movara.app.domain.recommendation.RecommendationSource
import ee.movara.app.ui.state.UiState
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
  private val dispatcher = UnconfinedTestDispatcher()
  private val models = mutableListOf<HomeViewModel>()
  private val repository = HomeRepository()
  private val taste = HomeTasteRepository()
  private val today = LocalDate.of(2026, 11, 9)
  private fun viewModel() = HomeViewModel(repository, taste) { today }.also { models += it }
  private fun HomeViewModel.ids() = uiState.value.recommendations.map { it.movie.id }

  @Before fun setUp() { Dispatchers.setMain(dispatcher) }
  @After fun tearDown() {
    models.forEach { it.viewModelScope.cancel() }
    Dispatchers.resetMain()
  }

  @Test fun skippedQuizWithoutLikesLoadsDiscovery() = runTest {
    taste.stored.value = MovieTasteProfile(onboardingStatus = OnboardingStatus.SKIPPED)
    val model = viewModel()
    assertFalse(model.uiState.value.loading)
    assertEquals(listOf(100), model.ids())
    assertEquals(100, model.uiState.value.topPick?.id)
    assertEquals(1, repository.trendingCalls)
    assertTrue(repository.recommendationCalls.isEmpty())
  }

  @Test fun oneThreeAndFiveQuizFavouritesWorkWithoutAnyLikes() = runTest {
    for (count in listOf(1, 3, 5)) {
      repository.recommendationCalls.clear()
      taste.stored.value = MovieTasteProfile(MovieTastePreferences(
        favouriteMovieIds = (1..count).toSet(),
        favouriteTitles = (1..count).associateWith { "Favourite $it" }
      ), OnboardingStatus.COMPLETED)
      val model = viewModel()
      assertTrue(model.ids().isNotEmpty())
      assertEquals((1..count).toList(), repository.recommendationCalls.distinct())
      assertEquals(count, model.uiState.value.recommendations.count { !it.isDiscovery })
      val seed = model.uiState.value.topRecommendation!!.sources
        .filterIsInstance<RecommendationSource.Seed>().single()
      assertEquals("Favourite 1", seed.title)
      model.viewModelScope.cancel()
    }
  }

  @Test fun oneThreeAndFiveLikesWorkAndRemovingTheLastLikeRestoresDiscovery() = runTest {
    val model = viewModel()
    for (count in listOf(1, 3, 5)) {
      repository.liked.value = success((1..count).map(::movie))
      assertEquals(count, model.uiState.value.recommendations.count { !it.isDiscovery })
      assertEquals(101, model.uiState.value.topPick?.id)
    }
    repository.liked.value = success(emptyList())
    assertEquals(listOf(100), model.ids())
  }

  @Test fun requestsAreBoundedAndDuplicateSeedsAreFetchedOnlyOnce() = runTest {
    repository.liked.value = success((1..7).map(::movie))
    taste.stored.value = MovieTasteProfile(MovieTastePreferences(
      genreIds = setOf(28, 35, 18, 27, 53),
      favouriteMovieIds = (1..10).toSet()
    ))
    repository.requestDelay = 100
    val model = viewModel()
    advanceUntilIdle()
    assertEquals((1..7).toList(), repository.recommendationCalls)
    assertEquals(5, repository.discoveryOptions.size)
    assertEquals(1, repository.trendingCalls)
    assertTrue(repository.maximumConcurrentRequests <= 3)
    assertTrue(model.ids().none { it in 1..10 })
  }

  @Test fun largeHistoryUsesTenLikesAndStaysWithinTwentyOneRequests() = runTest {
    repository.liked.value = success((1..30).map(::movie))
    taste.stored.value = MovieTasteProfile(MovieTastePreferences(
      genreIds = setOf(28, 35, 18, 27, 53),
      favouriteMovieIds = (1001..1005).toSet()
    ))
    repository.requestDelay = 100
    viewModel()
    advanceUntilIdle()
    assertEquals(15, repository.recommendationCalls.size)
    assertEquals(15, repository.recommendationCalls.toSet().size)
    assertTrue(repository.recommendationCalls.containsAll((1..5).toList()))
    assertEquals(5, repository.recommendationCalls.count { it in 6..30 })
    assertEquals(5, repository.discoveryOptions.size)
    assertEquals(1, repository.trendingCalls)
    assertTrue(repository.maximumConcurrentRequests <= 3)
  }

  @Test fun automaticLoadsPreferFreshPagesAndOnlyExplicitRetryBypassesThem() = runTest {
    val model = viewModel()
    assertEquals(listOf(MovieRequestPolicy.PREFER_FRESH_CACHE), repository.requestPolicies)
    repository.requestPolicies.clear()
    model.retry()
    assertEquals(listOf(MovieRequestPolicy.NETWORK_FIRST), repository.requestPolicies)
    repository.requestPolicies.clear()
    model.toggleLike(movie(200))
    assertTrue(repository.requestPolicies.isNotEmpty())
    assertTrue(repository.requestPolicies.all { it == MovieRequestPolicy.PREFER_FRESH_CACHE })
  }

  @Test fun freshCacheReuseDoesNotShowTheOfflineFallbackNotice() = runTest {
    repository.trendingResult = RepositoryResult.Success(
      page(movie(100)), DataSource.CACHE, freshCacheHit = true
    )
    val model = viewModel()
    assertEquals(listOf(100), model.ids())
    assertFalse(model.uiState.value.usingCache)
  }

  @Test fun genreAndRatingEditsRefreshQueriesAndTheMinimumIsNeverLowered() = runTest {
    taste.stored.value = MovieTasteProfile(MovieTastePreferences(genreIds = setOf(28)))
    val model = viewModel()
    assertEquals(28, repository.discoveryOptions.last().genreId)
    assertEquals(today, repository.discoveryOptions.last().releasedBy)
    assertEquals(MovieSort.POPULARITY, repository.discoveryOptions.last().sort)
    assertNull(repository.discoveryOptions.last().minimumRating)
    taste.stored.value = MovieTasteProfile(
      MovieTastePreferences(genreIds = setOf(35), minimumRating = 8)
    )
    assertEquals(35, repository.discoveryOptions.last().genreId)
    assertEquals(8, repository.discoveryOptions.last().minimumRating)
    assertTrue(model.uiState.value.recommendations.all { (it.movie.rating ?: 0.0) >= 8.0 })
    assertEquals(8, model.uiState.value.minimumRating)
  }

  @Test fun ratingOnlyProfileCanBeEmptyWithoutFallingBackBelowTheMinimum() = runTest {
    taste.stored.value = MovieTasteProfile(MovieTastePreferences(minimumRating = 8))
    repository.trendingResult = success(page(movie(100).copy(rating = 7.9)))
    val model = viewModel()
    assertTrue(model.ids().isEmpty())
    assertNull(model.uiState.value.topPick)
    assertFalse(model.uiState.value.loadFailed)
  }

  @Test fun watchedChangesRerankLocallyAndCanRestoreCandidates() = runTest {
    repository.liked.value = success(listOf(movie(1), movie(2)))
    val model = viewModel()
    val requests = repository.recommendationCalls.toList()
    repository.watched.value = success(listOf(movie(101)))
    assertFalse(101 in model.ids())
    assertEquals(102, model.uiState.value.topPick?.id)
    repository.watched.value = success(listOf(movie(100), movie(101), movie(102)))
    assertEquals(UiState.Empty, model.uiState.value.contentState)
    repository.watched.value = success(emptyList())
    assertEquals(101, model.uiState.value.topPick?.id)
    assertTrue(model.uiState.value.contentState is UiState.Success)
    assertEquals(requests, repository.recommendationCalls)
    assertEquals(1, repository.trendingCalls)
  }

  @Test fun watchedReadFailureHidesUnfilteredMoviesUntilRecovery() = runTest {
    repository.watched.value = failure
    val model = viewModel()
    assertTrue(model.uiState.value.loadFailed)
    assertTrue(model.ids().isEmpty())
    repository.watched.value = success(emptyList())
    assertFalse(model.uiState.value.loadFailed)
    assertEquals(100, model.uiState.value.topPick?.id)
  }

  @Test fun retryRestartsAWatchedObservationThatEndedAfterAReadFailure() = runTest {
    repository.watchedReadFailure = true
    val model = viewModel()
    assertTrue(model.uiState.value.loadFailed)
    assertTrue(model.ids().isEmpty())
    repository.watchedReadFailure = false
    repository.watched.value = success(listOf(movie(100)))
    model.retry()
    assertFalse(model.uiState.value.loadFailed)
    assertTrue(model.ids().isEmpty())
    repository.watched.value = success(emptyList())
    assertEquals(listOf(100), model.ids())
  }

  @Test fun missingProfileDoesNotSilentlyIgnoreRatingPreferencesAndRetryResubscribes() = runTest {
    taste.readFailure = IOException()
    val model = viewModel()
    assertTrue(model.uiState.value.loadFailed)
    assertTrue(model.ids().isEmpty())
    assertEquals(0, repository.trendingCalls)
    taste.readFailure = null
    taste.stored.value = MovieTasteProfile(MovieTastePreferences(minimumRating = 8))
    model.retry()
    assertEquals(listOf(100), model.ids())
    assertEquals(8, model.uiState.value.minimumRating)
  }

  @Test fun failedRequestCanBeRetried() = runTest {
    repository.trendingResult = failure
    val model = viewModel()
    assertTrue(model.uiState.value.loadFailed)
    repository.trendingResult = success(page(movie(100)))
    model.retry()
    assertFalse(model.uiState.value.loadFailed)
    assertEquals(100, model.uiState.value.topPick?.id)
    assertEquals(2, repository.trendingCalls)
  }

  @Test fun partialFailureKeepsOtherRecommendationsAndCacheIsReported() = runTest {
    repository.liked.value = success(listOf(movie(1), movie(2)))
    repository.failedRecommendationId = 1
    repository.trendingResult = RepositoryResult.Success(page(movie(100)), DataSource.CACHE)
    val model = viewModel()
    assertTrue(model.uiState.value.loadFailed)
    assertTrue(model.uiState.value.usingCache)
    assertEquals(102, model.uiState.value.topPick?.id)
  }

  @Test fun lateCancelledResponseCannotReplaceRecommendationsForNewPreferences() = runTest {
    repository.requestDelay = 1000
    repository.ignoreCancellation = true
    val model = viewModel()
    taste.stored.value = MovieTasteProfile(MovieTastePreferences(
      favouriteMovieIds = setOf(2), minimumRating = 8
    ))
    advanceUntilIdle()
    assertFalse(model.uiState.value.loading)
    assertEquals(8, model.uiState.value.minimumRating)
    assertEquals(102, model.uiState.value.topPick?.id)
  }

  @Test fun listActionsUpdateMembershipAndWatchLaterDoesNotRefetchRecommendations() = runTest {
    val model = viewModel()
    model.toggleWatchLater(movie(100))
    assertTrue(100 in model.uiState.value.watchLaterIds)
    assertEquals(1, repository.trendingCalls)
    model.toggleWatchLater(movie(100))
    assertFalse(100 in model.uiState.value.watchLaterIds)
    model.toggleLike(movie(100))
    assertTrue(100 in model.uiState.value.likedIds)
    assertFalse(100 in model.ids())
    model.toggleLike(movie(100))
    assertFalse(100 in model.uiState.value.likedIds)
  }

  @Test fun failedSaveReportsFeedbackWithoutSelectingMovie() = runTest {
    repository.failSave = true
    val model = viewModel()
    model.toggleWatchLater(movie(100))
    assertTrue(model.uiState.value.actionFailed)
    assertFalse(100 in model.uiState.value.watchLaterIds)
    model.dismissActionError()
    assertFalse(model.uiState.value.actionFailed)
  }
}

private fun movie(id: Int) = Movie(
  id, "Movie $id", null, 2020, setOf(28), 8.0, 1000, LocalDate.of(2020, 1, 1)
)
private fun page(vararg movies: Movie) = MoviePage(movies.toList(), 1, 1, movies.size)
private fun <T> success(data: T) = RepositoryResult.Success(data, DataSource.LOCAL)
private val failure = RepositoryResult.Error(ErrorCategory.NETWORK, "Unavailable")

private class HomeTasteRepository : MovieTasteRepository {
  val stored = MutableStateFlow(MovieTasteProfile())
  var readFailure: Exception? = null
  override val profile = flow {
    readFailure?.let { throw it }
    emitAll(stored)
  }
  override suspend fun savePreferences(preferences: MovieTastePreferences) {
    stored.value = MovieTasteProfile(preferences, OnboardingStatus.COMPLETED)
  }
  override suspend fun skipOnboarding() { error("Home must not change onboarding status") }
}

private class HomeRepository : MovieRepository {
  val liked = MutableStateFlow<RepositoryResult<List<Movie>>>(success(emptyList()))
  private val saved = MutableStateFlow<RepositoryResult<List<Movie>>>(success(emptyList()))
  val watched = MutableStateFlow<RepositoryResult<List<Movie>>>(success(emptyList()))
  var trendingResult: RepositoryResult<MoviePage> = success(page(movie(100)))
  var trendingCalls = 0
  val recommendationCalls = mutableListOf<Int>()
  val discoveryOptions = mutableListOf<MovieDiscovery>()
  var watchedReadFailure = false
  override fun observeWatchedMovies(): Flow<RepositoryResult<List<Movie>>> = flow {
    if (watchedReadFailure) emit(failure) else emitAll(watched)
  }

  var failedRecommendationId: Int? = null
  var failSave = false
  var requestDelay = 0L
  var ignoreCancellation = false
  var maximumConcurrentRequests = 0
  private var activeRequests = 0
  val requestPolicies = mutableListOf<MovieRequestPolicy>()

  override suspend fun trendingMovies(page: Int, policy: MovieRequestPolicy): RepositoryResult<MoviePage> {
    requestPolicies += policy
    return trendingMovies(page)
  }
  override suspend fun recommendedMovies(
    movieId: Int, page: Int, policy: MovieRequestPolicy
  ): RepositoryResult<MoviePage> {
    requestPolicies += policy
    return recommendedMovies(movieId, page)
  }
  override suspend fun discoverMovies(
    options: MovieDiscovery, page: Int, policy: MovieRequestPolicy
  ): RepositoryResult<MoviePage> {
    requestPolicies += policy
    return discoverMovies(options, page)
  }

  private suspend fun delayed(result: RepositoryResult<MoviePage>): RepositoryResult<MoviePage> {
    activeRequests++
    maximumConcurrentRequests = maxOf(maximumConcurrentRequests, activeRequests)
    try {
      if (ignoreCancellation) withContext(NonCancellable) { delay(requestDelay) }
      else delay(requestDelay)
      return result
    } finally {
      activeRequests--
    }
  }
  override suspend fun trendingMovies(page: Int): RepositoryResult<MoviePage> {
    trendingCalls++
    return delayed(trendingResult)
  }
  override suspend fun recommendedMovies(movieId: Int, page: Int): RepositoryResult<MoviePage> {
    recommendationCalls += movieId
    return delayed(if (movieId == failedRecommendationId) failure
      else success(page(movie(movieId + 100))))
  }
  override suspend fun discoverMovies(options: MovieDiscovery, page: Int): RepositoryResult<MoviePage> {
    discoveryOptions += options
    return delayed(success(page(movie(1000 + (options.genreId ?: 0)))))
  }
  override fun observeMovies(listType: MovieListType) =
    when (listType) {
      MovieListType.LIKED -> liked
      MovieListType.WATCH_LATER -> saved
      MovieListType.WATCHED -> watched
    }
  override suspend fun saveMovie(movie: Movie, listType: MovieListType): RepositoryResult<Unit> {
    if (failSave) return failure
    val flow = observeMovies(listType)
    val movies = (flow.value as RepositoryResult.Success).data
    flow.value = success(listOf(movie) + movies.filterNot { it.id == movie.id })
    return success(Unit)
  }
  override suspend fun removeMovie(movieId: Int, listType: MovieListType): RepositoryResult<Unit> {
    val flow = observeMovies(listType)
    flow.value = success((flow.value as RepositoryResult.Success).data.filterNot { it.id == movieId })
    return success(Unit)
  }
  override suspend fun searchMovies(title: String, page: Int): RepositoryResult<MoviePage> = error("Unused")
  override suspend fun moviesInGenre(genreId: Int, page: Int): RepositoryResult<MoviePage> = error("Unused")
  override suspend fun movieDetails(movieId: Int): RepositoryResult<MovieDetails> = error("Unused")
  override fun observeCount(listType: MovieListType) = observeMovies(listType).map {
    success((it as RepositoryResult.Success).data.size)
  }
  override fun observeMembership(movieId: Int, listType: MovieListType) = observeMovies(listType).map {
    success((it as RepositoryResult.Success).data.any { movie -> movie.id == movieId })
  }
}
