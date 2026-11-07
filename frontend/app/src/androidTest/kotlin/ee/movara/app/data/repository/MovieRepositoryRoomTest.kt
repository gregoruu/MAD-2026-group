package ee.movara.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ee.movara.app.data.local.MovaraDatabase
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.remote.tmdbJson
import ee.movara.app.domain.model.Movie
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class MovieRepositoryRoomTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  private fun repository(api: FakeTmdbApiService, db: MovaraDatabase) =
    DefaultMovieRepository(api, db.tmdbCacheDao(), db.savedMovieDao())

  @Test
  fun successfulResponsesAreStoredInRoomAndAvailableOffline() = runBlocking {
    val db = Room.inMemoryDatabaseBuilder(context, MovaraDatabase::class.java).build()
    try {
      val api = FakeTmdbApiService()
      val repository = repository(api, db)
      val fresh = repository.searchMovies("Matrix") as RepositoryResult.Success
      val entry = requireNotNull(db.tmdbCacheDao().get("search:matrix:1"))
      val serialized = tmdbJson.decodeFromString<ee.movara.app.data.remote.MoviePageDto>(entry.responseJson)
      assertEquals(603, serialized.results.single().id)
      assertEquals(fresh.fetchedAt, entry.fetchedAt)

      repository.searchMovies("Alien")
      repository.searchMovies("Matrix", 2)
      repository.moviesInGenre(28)
      repository.moviesInGenre(35)
      repository.moviesInGenre(28, 2)
      repository.trendingMovies()
      repository.recommendedMovies(603)
      repository.movieDetails(603)
      val before = db.tmdbCacheDao().get("search:matrix:1")

      api.failure = IOException()
      // A new repository proves that fallback uses Room rather than instance memory.
      val offline = repository(api, db)
      val cached = offline.searchMovies(" MATRIX ") as RepositoryResult.Success
      assertEquals(DataSource.CACHE, cached.source)
      assertEquals(fresh.data, cached.data)
      assertEquals(before, db.tmdbCacheDao().get("search:matrix:1"))
      for (result in listOf(
        offline.searchMovies("Alien"), offline.searchMovies("matrix", 2),
        offline.moviesInGenre(28), offline.moviesInGenre(35), offline.moviesInGenre(28, 2),
        offline.trendingMovies(), offline.recommendedMovies(603), offline.movieDetails(603)
      )) {
        assertEquals(DataSource.CACHE, (result as RepositoryResult.Success).source)
      }
      assertTrue(offline.searchMovies("Uncached") is RepositoryResult.Error)
      assertTrue(offline.moviesInGenre(99) is RepositoryResult.Error)
      assertTrue(offline.searchMovies("matrix", 3) is RepositoryResult.Error)
    } finally {
      db.close()
    }
  }

  @Test
  fun savedListsExposeDomainMoviesCountsMembershipAndReactiveUpdates() = runBlocking {
    val db = Room.inMemoryDatabaseBuilder(context, MovaraDatabase::class.java).build()
    try {
      val repository = repository(FakeTmdbApiService(), db)
      val matrix = Movie(603, "The Matrix", "/matrix.jpg", 1999)
      assertEquals(emptyList<Movie>(), (repository.observeLikedMovies().first() as RepositoryResult.Success).data)
      val update = async(Dispatchers.IO) {
        withTimeout(10.seconds) {
          repository.observeLikedMovies().filter {
            it is RepositoryResult.Success && it.data.isNotEmpty()
          }.first()
        }
      }
      for (list in MovieListType.entries) {
        assertTrue(repository.saveMovie(matrix, list) is RepositoryResult.Success)
      }
      assertEquals(listOf(matrix), (update.await() as RepositoryResult.Success).data)
      assertEquals(listOf(matrix), (repository.observeWatchLaterMovies().first() as RepositoryResult.Success).data)
      assertEquals(listOf(matrix), (repository.observeWatchedMovies().first() as RepositoryResult.Success).data)
      assertEquals(1, (repository.observeCount(MovieListType.LIKED).first() as RepositoryResult.Success).data)
      assertEquals(true, (repository.observeMembership(603, MovieListType.LIKED).first() as RepositoryResult.Success).data)

      repository.saveMovie(matrix.copy(title = "Updated"), MovieListType.LIKED)
      assertEquals(1, (repository.observeCount(MovieListType.LIKED).first() as RepositoryResult.Success).data)
      assertEquals("Updated", (repository.observeLikedMovies().first() as RepositoryResult.Success).data.single().title)
      repository.removeMovie(603, MovieListType.LIKED)
      assertEquals(false, (repository.observeMembership(603, MovieListType.LIKED).first() as RepositoryResult.Success).data)
      assertEquals(0, (repository.observeCount(MovieListType.LIKED).first() as RepositoryResult.Success).data)
      assertEquals(listOf(matrix), (repository.observeWatchedMovies().first() as RepositoryResult.Success).data)
      assertEquals(listOf(matrix), (repository.observeWatchLaterMovies().first() as RepositoryResult.Success).data)
      // Saving a list entry must never add or replace a TMDB cache entry.
      assertNull(db.tmdbCacheDao().get("details:603"))
    } finally {
      db.close()
    }
  }

  @Test
  fun cachedPagesAndDetailsSurvivePersistentDatabaseReopening() = runBlocking {
    val name = "repository-reopen-${System.nanoTime()}.db"
    fun open() = Room.databaseBuilder(context, MovaraDatabase::class.java, name)
      .addMigrations(MovaraDatabase.MIGRATION_1_2).build()
    try {
      val api = FakeTmdbApiService()
      val first = open()
      val expectedPage: Any?
      val expectedDetails: Any?
      try {
        val repository = repository(api, first)
        expectedPage = (repository.trendingMovies() as RepositoryResult.Success).data
        expectedDetails = (repository.movieDetails(603) as RepositoryResult.Success).data
      } finally {
        first.close()
      }
      api.failure = IOException()
      val reopened = open()
      try {
        val repository = repository(api, reopened)
        val page = repository.trendingMovies() as RepositoryResult.Success
        val details = repository.movieDetails(603) as RepositoryResult.Success
        assertEquals(expectedPage, page.data)
        assertEquals(expectedDetails, details.data)
        assertEquals(DataSource.CACHE, page.source)
        assertEquals(DataSource.CACHE, details.source)
      } finally {
        reopened.close()
      }
    } finally {
      context.deleteDatabase(name)
    }
  }

  @Test
  fun migrationFromExportedVersionOnePreservesAllSavedLists() = runBlocking(Dispatchers.IO) {
    val name = "repository-migration-${System.nanoTime()}.db"
    try {
      // Build the actual committed v1 schema, including its Room identity and indices.
      val assetContext = InstrumentationRegistry.getInstrumentation().context
      val schema = assetContext.assets.open("ee.movara.app.data.local.MovaraDatabase/1.json")
        .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
      context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
          val entity = entities.getJSONObject(i)
          val table = entity.getString("tableName")
          old.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", table))
          val indices = entity.getJSONArray("indices")
          for (j in 0 until indices.length()) {
            old.execSQL(indices.getJSONObject(j).getString("createSql").replace("${'$'}{TABLE_NAME}", table))
          }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
        for (list in MovieListType.entries) {
          old.execSQL(
            "INSERT INTO saved_movie (tmdbId, title, posterPath, year, listType, addedAt) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(603, "The Matrix", "/matrix.jpg", 1999, list.name, 123L)
          )
        }
        old.version = 1
      }
      // Room opens v1, executes the production migration and validates the whole v2 schema.
      val migrated = Room.databaseBuilder(context, MovaraDatabase::class.java, name)
        .addMigrations(MovaraDatabase.MIGRATION_1_2).build()
      try {
        val api = FakeTmdbApiService()
        val repository = repository(api, migrated)
        for (list in MovieListType.entries) {
          val entries = migrated.savedMovieDao().observeMovies(list).first()
          assertEquals(1, entries.size)
          assertEquals(603, entries.single().tmdbId)
          assertEquals("The Matrix", entries.single().title)
          assertEquals("/matrix.jpg", entries.single().posterPath)
          assertEquals(1999, entries.single().year)
          assertEquals(123L, entries.single().addedAt)
          assertEquals(list, entries.single().listType)
        }
        assertTrue(repository.movieDetails(603) is RepositoryResult.Success)
        assertNotNull(migrated.tmdbCacheDao().get("details:603"))
      } finally {
        migrated.close()
      }
    } finally {
      context.deleteDatabase(name)
    }
  }
}
