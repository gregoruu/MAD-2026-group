package ee.movara.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovaraDatabaseTest {

    private val context: Context =
        ApplicationProvider.getApplicationContext()

    private fun movie(
        id: Int,
        type: MovieListType,
        time: Long
    ) = SavedMovieEntity(
        tmdbId = id,
        title = "Movie $id",
        posterPath = null,
        year = null,
        listType = type,
        addedAt = time
    )

    @Test
    fun listsHaveCorrectOrderCountsAndIndependentMemberships() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            context,
            MovaraDatabase::class.java
        ).build()

        try {
            val dao = database.savedMovieDao()

            dao.upsert(movie(603, MovieListType.LIKED, 100L))
            dao.upsert(movie(604, MovieListType.LIKED, 200L))
            dao.upsert(movie(603, MovieListType.WATCHED, 300L))

            assertEquals(
                listOf(604, 603),
                dao.observeMovies(MovieListType.LIKED)
                    .first()
                    .map { it.tmdbId }
            )

            assertEquals(
                2,
                dao.observeCount(MovieListType.LIKED).first()
            )
            assertEquals(
                1,
                dao.observeCount(MovieListType.WATCHED).first()
            )
            assertEquals(
                0,
                dao.observeCount(MovieListType.WATCH_LATER).first()
            )

            // Updating an existing entry must not create a duplicate.
            dao.upsert(
                movie(603, MovieListType.LIKED, 400L)
                    .copy(title = "Updated title")
            )

            val liked = dao.observeMovies(MovieListType.LIKED).first()
            assertEquals(2, liked.size)
            assertEquals(603, liked.first().tmdbId)
            assertEquals("Updated title", liked.first().title)

            // Removing from one list must preserve the other membership.
            dao.remove(603, MovieListType.LIKED)

            assertEquals(
                listOf(604),
                dao.observeMovies(MovieListType.LIKED)
                    .first()
                    .map { it.tmdbId }
            )
            assertEquals(
                listOf(603),
                dao.observeMovies(MovieListType.WATCHED)
                    .first()
                    .map { it.tmdbId }
            )
            assertEquals(
                1,
                dao.observeCount(MovieListType.LIKED).first()
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun savedMovieSurvivesDatabaseReopening() = runBlocking {
        val name = "room-test-${System.nanoTime()}.db"
        val expected = movie(603, MovieListType.LIKED, 100L)

        try {
            val firstDatabase = Room.databaseBuilder(
                context,
                MovaraDatabase::class.java,
                name
            ).build()

            try {
                firstDatabase.savedMovieDao().upsert(expected)
            } finally {
                firstDatabase.close()
            }

            val reopenedDatabase = Room.databaseBuilder(
                context,
                MovaraDatabase::class.java,
                name
            ).build()

            try {
                val actual = reopenedDatabase.savedMovieDao()
                    .observeMovies(MovieListType.LIKED)
                    .first()

                assertEquals(listOf(expected), actual)
            } finally {
                reopenedDatabase.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}