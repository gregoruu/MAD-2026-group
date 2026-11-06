package ee.movara.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TmdbCacheDao {
  @Query("SELECT * FROM tmdb_cache WHERE cacheKey = :key")
  suspend fun get(key: String): TmdbCacheEntity?

  @Upsert
  suspend fun upsert(entry: TmdbCacheEntity)
}
