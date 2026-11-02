package ee.movara.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMovieDao {

    @Query(
        """
        SELECT * FROM saved_movie
        WHERE listType = :listType
        ORDER BY addedAt DESC, tmdbId ASC
        """
    )
    fun observeMovies(
        listType: MovieListType
    ): Flow<List<SavedMovieEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM saved_movie
        WHERE listType = :listType
        """
    )
    fun observeCount(listType: MovieListType): Flow<Int>

    @Upsert
    suspend fun upsert(movie: SavedMovieEntity)

    @Query(
        """
        DELETE FROM saved_movie
        WHERE tmdbId = :tmdbId AND listType = :listType
        """
    )
    suspend fun remove(tmdbId: Int, listType: MovieListType)
}