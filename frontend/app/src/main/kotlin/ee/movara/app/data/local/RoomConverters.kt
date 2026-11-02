package ee.movara.app.data.local

import androidx.room.TypeConverters

class RoomConverters {
    @TypeConverters
    fun fromMovieListType(value: MovieListType): String = value.name

    @TypeConverters
    fun toMovieListType(value: String): MovieListType = MovieListType.valueOf(value)
}