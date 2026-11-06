package ee.movara.app.data.repository

import ee.movara.app.data.remote.MovieDetailsDto
import ee.movara.app.data.remote.MoviePageDto
import ee.movara.app.data.remote.MovieSummaryDto
import ee.movara.app.data.remote.TmdbApiService

/** Never creates a Retrofit client or uses an API key. */
internal class FakeTmdbApiService : TmdbApiService {
  var failure: Exception? = null
  var calls = 0
  var pageResponse = MoviePageDto(
    page = 1, results = listOf(MovieSummaryDto(603, "The Matrix")),
    totalPages = 3, totalResults = 45
  )
  var detailsResponse = MovieDetailsDto(id = 603, title = "The Matrix")
  var lastQuery: String? = null

  private fun requested() {
    calls++
    failure?.let { throw it }
  }

  override suspend fun searchMovies(title: String, page: Int): MoviePageDto {
    requested()
    lastQuery = title
    return pageResponse.copy(page = page)
  }
  override suspend fun trendingMovies(page: Int): MoviePageDto {
    requested()
    return pageResponse.copy(page = page)
  }
  override suspend fun recommendedMovies(movieId: Int, page: Int): MoviePageDto {
    requested()
    return pageResponse.copy(page = page)
  }
  override suspend fun moviesInGenre(genreId: Int, page: Int): MoviePageDto {
    requested()
    return pageResponse.copy(page = page)
  }
  override suspend fun movieDetails(movieId: Int): MovieDetailsDto {
    requested()
    return detailsResponse.copy(id = movieId)
  }
}
