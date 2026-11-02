package ee.movara.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SavedMovieEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class MovaraDatabase : RoomDatabase() {
    abstract fun savedMovieDao(): SavedMovieDao
}