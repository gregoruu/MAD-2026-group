package ee.movara.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.DataSource
import ee.movara.app.data.repository.ErrorCategory
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieLists
import ee.movara.app.ui.movies.MovieListsViewModel
import ee.movara.app.domain.model.MovieDetails
import ee.movara.app.domain.model.MoviePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
  private val dispatcher = UnconfinedTestDispatcher()
  private val models = mutableListOf<ViewModel>()
  private val repository = HomeRepository()
  private fun viewModel(): HomeViewModel {
    val lists = MovieListsViewModel(repository).also { models += it }
    return HomeViewModel(repository, lists.store).also { models += it }
  }

  @Before fun setUp() { Dispatchers.setMain(dispatcher) }
  @After fun tearDown() {
    models.forEach { it.viewModelScope.cancel() }
    Dispatchers.resetMain()
  }

  @Test fun noLikesLoadsTrending() = runTest {
    val model = viewModel()
    assertFalse(model.uiState.value.loading)
    assertEquals(listOf(HomeRail(movies = listOf(movie(100)))), model.uiState.value.rails)
    assertEquals(100, model.uiState.value.topPick?.id)
    assertEquals(1, repository.trendingCalls)
    assertTrue(repository.recommendationCalls.isEmpty())
  }

  @Test fun recommendationsUseAtMostFiveRecentLikes() = runTest {
    repository.liked.value = success((1..7).map(::movie))
    val model = viewModel()
    assertEquals((1..5).toList(), repository.recommendationCalls)
    assertEquals((1..5).toList(), model.uiState.value.rails.map { it.sourceMovie?.id })
    assertEquals(0, repository.trendingCalls)
  }

  @Test fun oneOrThreeLikesProduceMatchingRailsAndRemovingLastLikeRestoresTrending() = runTest {
    repository.liked.value = success(listOf(movie(1)))
    val model = viewModel()
    assertEquals(1, model.uiState.value.rails.size)
    repository.liked.value = success((1..3).map(::movie))
    assertEquals(3, model.uiState.value.rails.size)
    repository.liked.value = success(emptyList())
    assertNull(model.uiState.value.rails.single().sourceMovie)
    assertEquals(100, model.uiState.value.topPick?.id)
  }

  @Test fun watchedMoviesAreExcludedFromEveryRecommendationRailAndTopPick() = runTest {
    repository.liked.value = success(listOf(movie(1), movie(2), movie(3)))
    repository.watched.value = success(listOf(movie(101), movie(103)))
    val model = viewModel()
    assertEquals(listOf(emptyList<Int>(), listOf(102), emptyList<Int>()),
      model.uiState.value.rails.map { rail -> rail.movies.map(Movie::id) })
    assertEquals(102, model.uiState.value.topPick?.id)
  }

  @Test fun watchedChangesRefilterTrendingWithoutRefetchingAndCanRestoreMovies() = runTest {
    repository.trendingResult = success(page(movie(100), movie(101)))
    repository.watched.value = success(listOf(movie(100)))
    val model = viewModel()
    assertEquals(listOf(movie(101)), model.uiState.value.rails.single().movies)
    assertEquals(101, model.uiState.value.topPick?.id)
    repository.watched.value = success(listOf(movie(100), movie(101)))
    assertTrue(model.uiState.value.rails.single().movies.isEmpty())
    assertNull(model.uiState.value.topPick)
    repository.watched.value = success(emptyList())
    assertEquals(listOf(movie(100), movie(101)), model.uiState.value.rails.single().movies)
    assertEquals(100, model.uiState.value.topPick?.id)
    assertEquals(1, repository.trendingCalls)
  }

  @Test fun watchedChangesRefilterRecommendationsWithoutRefetching() = runTest {
    repository.liked.value = success(listOf(movie(1), movie(2)))
    val model = viewModel()
    repository.watched.value = success(listOf(movie(101)))
    assertEquals(102, model.uiState.value.topPick?.id)
    repository.watched.value = success(emptyList())
    assertEquals(101, model.uiState.value.topPick?.id)
    assertEquals(listOf(1, 2), repository.recommendationCalls)
  }

  @Test fun watchedReadFailureHidesUnfilteredMoviesUntilRecovery() = runTest {
    repository.watched.value = failure
    val model = viewModel()
    assertTrue(model.uiState.value.loadFailed)
    assertTrue(model.uiState.value.rails.isEmpty())
    assertNull(model.uiState.value.topPick)
    repository.watched.value = success(emptyList())
    assertFalse(model.uiState.value.loadFailed)
    assertEquals(100, model.uiState.value.topPick?.id)
  }

  @Test fun failedRequestCanBeRetried() = runTest {
    repository.trendingResult = failure
    val model = viewModel()
    assertTrue(model.uiState.value.rails.single().failed)
    repository.trendingResult = success(page(movie(100)))
    model.retry()
    assertFalse(model.uiState.value.rails.single().failed)
    assertEquals(100, model.uiState.value.topPick?.id)
    assertEquals(2, repository.trendingCalls)
  }

  @Test fun oneFailedRailDoesNotHideOtherRecommendations() = runTest {
    repository.liked.value = success(listOf(movie(1), movie(2)))
    repository.failedRecommendationId = 1
    val model = viewModel()
    assertTrue(model.uiState.value.rails.first().failed)
    assertEquals(102, model.uiState.value.topPick?.id)
  }

  @Test fun listActionsPersistAndReflectObservedMembership() = runTest {
    val model = viewModel()
    model.toggleWatchLater(movie(100))
    assertTrue(100 in model.uiState.value.watchLaterIds)
    assertEquals(1, repository.trendingCalls)
    model.toggleWatchLater(movie(100))
    assertFalse(100 in model.uiState.value.watchLaterIds)
    model.toggleLike(movie(100))
    assertTrue(100 in model.uiState.value.likedIds)
    model.toggleLike(movie(100))
    assertFalse(100 in model.uiState.value.likedIds)
  }

  @Test fun failedSaveReportsFeedbackWithoutSelectingMovie() = runTest {
    repository.failSave = true
    val model = viewModel()
    model.toggleWatchLater(movie(100))
    assertFalse(model.uiState.value.loadFailed)
    assertTrue(model.uiState.value.actionsEnabled)
    assertEquals(listOf(movie(100)), model.uiState.value.rails.flatMap { it.movies })
    assertFalse(100 in model.uiState.value.watchLaterIds)
    repository.failSave = false
    model.retry()
    assertFalse(model.uiState.value.loadFailed)
    assertTrue(model.uiState.value.actionsEnabled)
  }
}

private fun movie(id: Int) = Movie(id, "Movie $id", null, 2020)
private fun page(vararg movies: Movie) = MoviePage(movies.toList(), 1, 1, movies.size)
private fun <T> success(data: T) = RepositoryResult.Success(data, DataSource.LOCAL)
private val failure = RepositoryResult.Error(ErrorCategory.NETWORK, "Unavailable")

private class HomeRepository : MovieRepository {
  override suspend fun discoverMovies(
    options: ee.movara.app.domain.model.MovieDiscovery,
    page: Int
  ): RepositoryResult<MoviePage> = error("Unused")

  val liked = MutableStateFlow<RepositoryResult<List<Movie>>>(success(emptyList()))
  private val saved = MutableStateFlow<RepositoryResult<List<Movie>>>(success(emptyList()))
  val watched = MutableStateFlow<RepositoryResult<List<Movie>>>(success(emptyList()))
  var trendingResult: RepositoryResult<MoviePage> = success(page(movie(100)))
  var trendingCalls = 0
  val recommendationCalls = mutableListOf<Int>()
  var failedRecommendationId: Int? = null
  var failSave = false

  override suspend fun trendingMovies(page: Int): RepositoryResult<MoviePage> {
    trendingCalls++
    return trendingResult
  }
  override suspend fun recommendedMovies(movieId: Int, page: Int): RepositoryResult<MoviePage> {
    recommendationCalls += movieId
    return if (movieId == failedRecommendationId) failure else success(page(movie(movieId + 100)))
  }
  override fun observeLists() = combine(liked, saved, watched) { likes, later, seen ->
    when {
      likes is RepositoryResult.Error -> likes
      later is RepositoryResult.Error -> later
      seen is RepositoryResult.Error -> seen
      else -> success(MovieLists(
        liked = (likes as RepositoryResult.Success).data,
        watchLater = (later as RepositoryResult.Success).data,
        watched = (seen as RepositoryResult.Success).data
      ))
    }
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
