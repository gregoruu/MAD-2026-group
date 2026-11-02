package ee.movara.app.data.local

import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter
    fun fromMovieListType(value: MovieListType): String = value.name

    @TypeConverter
    fun toMovieListType(value: String): MovieListType = MovieListType.valueOf(value)
}