package ee.movara.app.domain.model

import ee.movara.app.data.local.MovieListType

/** A single consistent snapshot of the three local lists. */
data class MovieLists(
  val liked: List<Movie> = emptyList(),
  val watchLater: List<Movie> = emptyList(),
  val watched: List<Movie> = emptyList()
) {
  fun movies(type: MovieListType): List<Movie> = when (type) {
    MovieListType.LIKED -> liked
    MovieListType.WATCH_LATER -> watchLater
    MovieListType.WATCHED -> watched
  }

  fun contains(movieId: Int, type: MovieListType) = movies(type).any { it.id == movieId }
}
