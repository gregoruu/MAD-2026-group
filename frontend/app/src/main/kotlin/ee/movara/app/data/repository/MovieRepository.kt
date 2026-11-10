package ee.movara.app.data.repository

import ee.movara.app.data.local.MovieListType
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieDetails
import ee.movara.app.domain.model.MovieDiscovery
import ee.movara.app.domain.model.MoviePage
import ee.movara.app.domain.model.MovieLists
import kotlinx.coroutines.flow.Flow

/** Shared movie access for Home, Search, Genres, Details and My Movies ViewModels. */
interface MovieRepository {
  suspend fun searchMovies(title: String, page: Int = 1): RepositoryResult<MoviePage>
  suspend fun trendingMovies(page: Int = 1): RepositoryResult<MoviePage>
  suspend fun recommendedMovies(movieId: Int, page: Int = 1): RepositoryResult<MoviePage>
  suspend fun moviesInGenre(genreId: Int, page: Int = 1): RepositoryResult<MoviePage>
  suspend fun movieDetails(movieId: Int): RepositoryResult<MovieDetails>
  suspend fun discoverMovies(options: MovieDiscovery, page: Int = 1): RepositoryResult<MoviePage>

  /** Policy overloads allow Home to reuse fresh responses without changing other screens. */
  suspend fun trendingMovies(
    page: Int = 1,
    policy: MovieRequestPolicy
  ): RepositoryResult<MoviePage> = trendingMovies(page)
  suspend fun recommendedMovies(
    movieId: Int,
    page: Int = 1,
    policy: MovieRequestPolicy
  ): RepositoryResult<MoviePage> = recommendedMovies(movieId, page)
  suspend fun discoverMovies(
    options: MovieDiscovery,
    page: Int = 1,
    policy: MovieRequestPolicy
  ): RepositoryResult<MoviePage> = discoverMovies(options, page)

  fun observeMovies(listType: MovieListType): Flow<RepositoryResult<List<Movie>>>
  fun observeLists(): Flow<RepositoryResult<MovieLists>>
  fun observeLikedMovies() = observeMovies(MovieListType.LIKED)
  fun observeWatchLaterMovies() = observeMovies(MovieListType.WATCH_LATER)
  fun observeWatchedMovies() = observeMovies(MovieListType.WATCHED)
  fun observeCount(listType: MovieListType): Flow<RepositoryResult<Int>>
  fun observeMembership(movieId: Int, listType: MovieListType): Flow<RepositoryResult<Boolean>>
  suspend fun saveMovie(movie: Movie, listType: MovieListType): RepositoryResult<Unit>
  suspend fun removeMovie(movieId: Int, listType: MovieListType): RepositoryResult<Unit>
}
