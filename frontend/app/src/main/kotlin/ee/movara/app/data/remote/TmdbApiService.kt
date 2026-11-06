package ee.movara.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The TMDB endpoints Movara calls.
 *
 * Every function suspends and returns a parsed response. A request that fails, or that TMDB
 * answers with a non-2xx status, throws; the repository turns that into screen state.
 *
 * Paths are relative to [TMDB_BASE_URL] and carry no API key. [TmdbApiKeyInterceptor] adds
 * the key to every request.
 *
 * TMDB pages are 20 movies long and [MoviePageDto] reports how many pages exist.
 */
interface TmdbApiService {

  /** Movies whose title matches [title]. */
  @GET("search/movie")
  suspend fun searchMovies(
    @Query("query") title: String,
    @Query("page") page: Int
  ): MoviePageDto

  /** The movies TMDB lists as trending over the past week. */
  @GET("trending/movie/week")
  suspend fun trendingMovies(
    @Query("page") page: Int
  ): MoviePageDto

  /** Movies TMDB recommends to someone who liked the movie [movieId]. */
  @GET("movie/{movieId}/recommendations")
  suspend fun recommendedMovies(
    @Path("movieId") movieId: Int,
    @Query("page") page: Int
  ): MoviePageDto

  /** The movies of one TMDB genre, most popular first. */
  @GET("discover/movie?sort_by=popularity.desc")
  suspend fun moviesInGenre(
    @Query("with_genres") genreId: Int,
    @Query("page") page: Int
  ): MoviePageDto

  /**
   * One movie with its cast and crew in the same response, which arrives in
   * [MovieDetailsDto.credits].
   */
  @GET("movie/{movieId}?append_to_response=credits")
  suspend fun movieDetails(
    @Path("movieId") movieId: Int
  ): MovieDetailsDto
}
