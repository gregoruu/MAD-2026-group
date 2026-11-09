package ee.movara.app.ui.selectedgenre

import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.DataSource
import ee.movara.app.data.repository.ErrorCategory
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieDetails
import ee.movara.app.domain.model.MoviePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class SelectedGenreViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun movies(startId: Int, count: Int): List<Movie> =
        (startId until startId + count).map { id ->
            Movie(
                id = id,
                title = "Test Movie $id",
                posterPath = null,
                releaseYear = 2025
            )
        }

    private fun successfulPage(
        page: Int,
        totalPages: Int,
        movieList: List<Movie>
    ): RepositoryResult<MoviePage> {
        return RepositoryResult.Success(
            data = MoviePage(
                movies = movieList,
                page = page,
                totalPages = totalPages,
                totalResults = totalPages * 20
            ),
            source = DataSource.NETWORK
        )
    }

    @Test
    fun firstPageLoads20Movies() = runTest {
        val repository = FakeMovieRepository()

        repository.responses[1] = successfulPage(
            page = 1,
            totalPages = 2,
            movieList = movies(1, 20)
        )

        val viewModel = SelectedGenreViewModel(
            genreId = 28,
            movieRepository = repository
        )

        advanceUntilIdle()

        assertEquals(20, viewModel.uiState.value.movies.size)
        assertTrue(viewModel.uiState.value.hasMore)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(1), repository.requestedPages)
        assertEquals(listOf(28), repository.requestedGenres)
    }

    @Test
    fun nextPageAdds20MoviesWithoutDuplicates() = runTest {
        val repository = FakeMovieRepository()

        repository.responses[1] = successfulPage(
            page = 1,
            totalPages = 2,
            movieList = movies(1, 20)
        )
        repository.responses[2] = successfulPage(
            page = 2,
            totalPages = 2,
            movieList = movies(21, 20)
        )

        val viewModel = SelectedGenreViewModel(
            genreId = 28,
            movieRepository = repository
        )

        advanceUntilIdle()

        viewModel.loadMore()

        viewModel.loadMore()

        advanceUntilIdle()

        assertEquals(40, viewModel.uiState.value.movies.size)
        assertEquals(
            40,
            viewModel.uiState.value.movies.distinctBy { it.id }.size
        )
        assertEquals(listOf(1, 2), repository.requestedPages)
        assertFalse(viewModel.uiState.value.hasMore)

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.requestedPages)
    }

    @Test
    fun firstPageErrorCanBeRetried() = runTest {
        val repository = FakeMovieRepository()

        repository.responses[1] = RepositoryResult.Error(
            category = ErrorCategory.NETWORK,
            message = "Network error"
        )

        val viewModel = SelectedGenreViewModel(
            genreId = 35,
            movieRepository = repository
        )

        advanceUntilIdle()

        assertEquals(
            "Network error",
            viewModel.uiState.value.errorMessage
        )
        assertTrue(viewModel.uiState.value.movies.isEmpty())

        repository.responses[1] = successfulPage(
            page = 1,
            totalPages = 1,
            movieList = movies(1, 20)
        )

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(20, viewModel.uiState.value.movies.size)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(1, 1), repository.requestedPages)
    }

    @Test
    fun paginationErrorKeepsExistingMoviesAndRetries() = runTest {
        val repository = FakeMovieRepository()

        repository.responses[1] = successfulPage(
            page = 1,
            totalPages = 2,
            movieList = movies(1, 20)
        )
        repository.responses[2] = RepositoryResult.Error(
            category = ErrorCategory.NETWORK,
            message = "Could not load next page"
        )

        val viewModel = SelectedGenreViewModel(
            genreId = 28,
            movieRepository = repository
        )

        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(20, viewModel.uiState.value.movies.size)
        assertEquals(
            "Could not load next page",
            viewModel.uiState.value.loadMoreError
        )

        repository.responses[2] = successfulPage(
            page = 2,
            totalPages = 2,
            movieList = movies(21, 20)
        )

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(40, viewModel.uiState.value.movies.size)
        assertNull(viewModel.uiState.value.loadMoreError)
        assertEquals(listOf(1, 2, 2), repository.requestedPages)
    }
}

private class FakeMovieRepository : MovieRepository {
  override suspend fun discoverMovies(
    options: ee.movara.app.domain.model.MovieDiscovery,
    page: Int
  ): RepositoryResult<MoviePage> = error("Unused")


    val responses =
        mutableMapOf<Int, RepositoryResult<MoviePage>>()

    val requestedPages = mutableListOf<Int>()
    val requestedGenres = mutableListOf<Int>()

    override suspend fun moviesInGenre(
        genreId: Int,
        page: Int
    ): RepositoryResult<MoviePage> {
        requestedGenres.add(genreId)
        requestedPages.add(page)

        return responses[page] ?: unused()
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

    override suspend fun movieDetails(
        movieId: Int
    ): RepositoryResult<MovieDetails> = unused()

    override fun observeMovies(
        listType: MovieListType
    ): Flow<RepositoryResult<List<Movie>>> =
        flowOf(unused())

    override fun observeCount(
        listType: MovieListType
    ): Flow<RepositoryResult<Int>> =
        flowOf(unused())

    override fun observeMembership(
        movieId: Int,
        listType: MovieListType
    ): Flow<RepositoryResult<Boolean>> =
        flowOf(unused())

    override suspend fun saveMovie(
        movie: Movie,
        listType: MovieListType
    ): RepositoryResult<Unit> = unused()

    override suspend fun removeMovie(
        movieId: Int,
        listType: MovieListType
    ): RepositoryResult<Unit> = unused()
}
