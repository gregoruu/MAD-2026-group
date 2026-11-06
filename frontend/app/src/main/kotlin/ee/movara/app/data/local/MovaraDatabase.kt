package ee.movara.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedMovieEntity::class, TmdbCacheEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class MovaraDatabase : RoomDatabase() {
    abstract fun savedMovieDao(): SavedMovieDao
    abstract fun tmdbCacheDao(): TmdbCacheDao

    companion object {
        /** Add the response cache without touching existing saved_movie rows. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tmdb_cache " +
                        "(cacheKey TEXT NOT NULL, responseJson TEXT NOT NULL, " +
                        "fetchedAt INTEGER NOT NULL, PRIMARY KEY(cacheKey))"
                )
            }
        }
    }
}