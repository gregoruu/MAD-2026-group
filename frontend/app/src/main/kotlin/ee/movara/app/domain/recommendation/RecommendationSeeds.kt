package ee.movara.app.domain.recommendation

import ee.movara.app.domain.model.Movie
import java.time.LocalDate

/** Room supplies Likes newest first. Keep recent interests and rotate through the older history. */
object RecommendationSeeds {
  private const val RECENT_COUNT = 5
  private const val OLDER_COUNT = 5

  fun selectLikes(likedMovies: List<Movie>, today: LocalDate): List<Movie> {
    val distinct = likedMovies.distinctBy(Movie::id)
    val recent = distinct.take(RECENT_COUNT)
    val older = distinct.drop(RECENT_COUNT).sortedBy(Movie::id)
    if (older.size <= OLDER_COUNT) return recent + older
    // Consecutive five-film windows cover the whole history; UTC date keeps a day's choice stable.
    val offset = Math.floorMod(today.toEpochDay() * OLDER_COUNT, older.size.toLong()).toInt()
    return recent + List(OLDER_COUNT) { older[(offset + it) % older.size] }
  }
}
