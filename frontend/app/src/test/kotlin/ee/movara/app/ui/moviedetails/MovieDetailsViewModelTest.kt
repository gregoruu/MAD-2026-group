
package ee.movara.app.ui.moviedetails

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
import ee.movara.app.domain.model.MovieDiscovery
import ee.movara.app.domain.model.MoviePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailsViewModelTest {
    private val models = mutableListOf<ViewModel>()
    private fun viewModel(repository: MovieRepository): MovieDetailsViewModel {
        val lists = MovieListsViewModel(repository).also { models += it }
        return MovieDetailsViewModel(123, repository, lists.store).also { models += it }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        models.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private val details = MovieDetails(
        id = 123,
        title = "Test Movie",
        releaseYear = 2026,
        runtimeMinutes = 120,
        rating = 8.5,
        genres = emptyList(),
        director = "Test Director",
        overview = "A test movie description.",
        posterPath = null,
        backdropPath = null,
        topCast = listOf("Test Actor")
    )

    @Test
    fun loadsMovieDetailsSuccessfully() = runTest {
        val repository = FakeDetailsRepository(details)
        val viewModel = viewModel(repository)

        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(details, viewModel.uiState.value.movie)
        assertEquals(listOf(123), repository.requestedMovieIds)
    }

    @Test
    fun loadErrorCanBeRetried() = runTest {
        val repository = FakeDetailsRepository(details)

        repository.detailsResult = RepositoryResult.Error(
            category = ErrorCategory.NETWORK,
            message = "Network error"
        )

        val viewModel = viewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "Network error",
            viewModel.uiState.value.errorMessage
        )
        assertNull(viewModel.uiState.value.movie)

        repository.detailsResult = success(details)

        viewModel.loadMovie()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(details, viewModel.uiState.value.movie)
        assertEquals(
            listOf(123, 123),
            repository.requestedMovieIds
        )
    }

    @Test
    fun savedStatesAreObservedFromRepository() = runTest {
        val repository = FakeDetailsRepository(details)
        val viewModel = viewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLiked)
        assertFalse(viewModel.uiState.value.isWatchLater)
        assertFalse(viewModel.uiState.value.isWatched)

        repository.setMembership(MovieListType.LIKED, true)
        repository.setMembership(MovieListType.WATCH_LATER, true)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLiked)
        assertTrue(viewModel.uiState.value.isWatchLater)
        assertFalse(viewModel.uiState.value.isWatched)
    }

    @Test
    fun likeButtonSavesAndRemovesMovie() = runTest {
        val repository = FakeDetailsRepository(details)
        val viewModel = viewModel(repository)

        advanceUntilIdle()

        viewModel.toggleLike()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLiked)
        assertEquals(
            listOf(123 to MovieListType.LIKED),
            repository.saved
        )

        viewModel.toggleLike()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLiked)
        assertEquals(
            listOf(123 to MovieListType.LIKED),
            repository.removed
        )
    }

    @Test
    fun markingWatchedRemovesWatchLater() = runTest {
        val repository = FakeDetailsRepository(details)
        repository.setMembership(
            MovieListType.WATCH_LATER,
            true
        )

        val viewModel = viewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isWatchLater)

        viewModel.toggleWatched()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isWatched)
        assertFalse(viewModel.uiState.value.isWatchLater)

        assertTrue(
            repository.saved.contains(
                123 to MovieListType.WATCHED
            )
        )

        assertTrue(
            repository.removed.contains(
                123 to MovieListType.WATCH_LATER
            )
        )
    }

    @Test
    fun saveFailureShowsErrorWithoutChangingMembership() = runTest {
        val repository = FakeDetailsRepository(details)
        repository.failSave = true

        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.toggleLike()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLiked)
        assertEquals(
            "Could not save movie",
            models.filterIsInstance<MovieListsViewModel>().single().feedback.first().error
        )

        assertNull(viewModel.uiState.value.actionError)
        assertTrue(viewModel.uiState.value.actionsEnabled)
        repository.failSave = false
        viewModel.toggleLike()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.actionsEnabled)
        assertTrue(viewModel.uiState.value.isLiked)

        assertNull(viewModel.uiState.value.actionError)
    }

    @Test
    fun pendingActionBlocksFurtherTapsUntilRoomConfirms() = runTest {
        val repository = FakeDetailsRepository(details)
        val viewModel = viewModel(repository)

        advanceUntilIdle()

        // Do not wait between actions.
        viewModel.toggleLike()
        viewModel.toggleWatchLater()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLiked)
        assertFalse(viewModel.uiState.value.isWatchLater)
        viewModel.toggleWatchLater()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isWatchLater)

        assertEquals(
            listOf(
                123 to MovieListType.LIKED,
                123 to MovieListType.WATCH_LATER
            ),
            repository.saved
        )
    }
}

private fun <T> success(data: T): RepositoryResult.Success<T> =
    RepositoryResult.Success(
        data = data,
        source = DataSource.LOCAL
    )

private class FakeDetailsRepository(
    details: MovieDetails
) : MovieRepository {

    var detailsResult: RepositoryResult<MovieDetails> =
        success(details)

    var failSave = false

    val requestedMovieIds = mutableListOf<Int>()
    val saved = mutableListOf<Pair<Int, MovieListType>>()
    val removed = mutableListOf<Pair<Int, MovieListType>>()

    private val snapshots = MutableStateFlow<RepositoryResult<MovieLists>>(success(MovieLists()))
    private val movie = Movie(details.id, details.title, details.posterPath, details.releaseYear)
    override fun observeLists(): Flow<RepositoryResult<MovieLists>> = snapshots

    private val memberships =
        MovieListType.entries.associateWith {
            MutableStateFlow<RepositoryResult<Boolean>>(
                success(false)
            )
        }

    fun setMembership(
        type: MovieListType,
        selected: Boolean
    ) {
        memberships.getValue(type).value = success(selected)
        val lists = (snapshots.value as RepositoryResult.Success).data
        fun updated() = if (selected) listOf(movie) else emptyList()
        snapshots.value = success(when (type) {
            MovieListType.LIKED -> lists.copy(liked = updated())
            MovieListType.WATCH_LATER -> lists.copy(watchLater = updated())
            MovieListType.WATCHED -> lists.copy(watched = updated())
        })
    }

    override suspend fun movieDetails(
        movieId: Int
    ): RepositoryResult<MovieDetails> {
        requestedMovieIds.add(movieId)
        return detailsResult
    }

    override fun observeMembership(
        movieId: Int,
        listType: MovieListType
    ): Flow<RepositoryResult<Boolean>> =
        memberships.getValue(listType)

    override suspend fun saveMovie(
        movie: Movie,
        listType: MovieListType
    ): RepositoryResult<Unit> {
        if (failSave) {
            return RepositoryResult.Error(
                ErrorCategory.DATABASE,
                "Could not save movie"
            )
        }

        saved.add(movie.id to listType)
        setMembership(listType, true)
        if (listType == MovieListType.WATCHED) removeMovie(movie.id, MovieListType.WATCH_LATER)

        return success(Unit)
    }

    override suspend fun removeMovie(
        movieId: Int,
        listType: MovieListType
    ): RepositoryResult<Unit> {
        removed.add(movieId to listType)
        setMembership(listType, false)

        return success(Unit)
    }

    private fun unused() = RepositoryResult.Error(
        ErrorCategory.UNEXPECTED,
        "Not used in this test"
    )

    override suspend fun searchMovies(
        title: String,
        page: Int
    ): RepositoryResult<MoviePage> = unused()

    override suspend fun trendingMovies(
        page: Int
    ): RepositoryResult<MoviePage> = unused()

    override suspend fun recommendedMovies(
        movieId: Int,
        page: Int
    ): RepositoryResult<MoviePage> = unused()

    override suspend fun moviesInGenre(
        genreId: Int,
        page: Int
    ): RepositoryResult<MoviePage> = unused()

    override suspend fun discoverMovies(
        options: MovieDiscovery,
        page: Int
    ): RepositoryResult<MoviePage> = unused()

    override fun observeMovies(
        listType: MovieListType
    ): Flow<RepositoryResult<List<Movie>>> =
        flowOf(unused())

    override fun observeCount(
        listType: MovieListType
    ): Flow<RepositoryResult<Int>> =
        flowOf(unused())

}
