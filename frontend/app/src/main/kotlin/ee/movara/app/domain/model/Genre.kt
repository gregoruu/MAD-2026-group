package ee.movara.app.domain.model

/**
 * A TMDB genre. The Genres screen shows a fixed list of ten, each tied to the genre
 * id TMDB uses; Movie Details shows the genres of one movie.
 */
data class Genre(
  val id: Int,
  val name: String
)
