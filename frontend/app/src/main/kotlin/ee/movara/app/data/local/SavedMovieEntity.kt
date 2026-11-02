package ee.movara.app.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "saved_movie",
    primaryKeys = ["tmdbId", "listType"],
    indices = [Index(value = ["listType", "addedAt"])]
)

data class SavedMovieEntity (
    val tmdbId: Int,
    val title: String,
    val posterPath: String?,
    val year: Int?,
    val listType: MovieListType,
    val addedAt: Long
)