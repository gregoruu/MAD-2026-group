package ee.movara.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** TMDB responses are independent of the user's saved lists. */
@Entity(tableName = "tmdb_cache")
data class TmdbCacheEntity(
  @PrimaryKey val cacheKey: String,
  val responseJson: String,
  val fetchedAt: Long
)
