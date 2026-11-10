package ee.movara.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMovieDao {

    @Transaction
    suspend fun markWatched(movie: SavedMovieEntity) {
        require(movie.listType == MovieListType.WATCHED)
        upsert(movie)
        remove(movie.tmdbId, MovieListType.WATCH_LATER)
    }

    // One query gives every screen a consistent snapshot after list transactions.
    @Query("SELECT * FROM saved_movie ORDER BY addedAt DESC, tmdbId ASC")
    fun observeAllMovies(): Flow<List<SavedMovieEntity>>

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

    @Query("SELECT EXISTS(SELECT 1 FROM saved_movie WHERE tmdbId = :movieId AND listType = :listType)")
    fun observeMembership(movieId: Int, listType: MovieListType): Flow<Boolean>

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
