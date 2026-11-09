package ee.movara.app.data.repository

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import ee.movara.app.domain.model.MovieTastePreferences
import ee.movara.app.domain.model.MovieTasteProfile
import ee.movara.app.domain.model.OnboardingStatus
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreMovieTasteRepositoryTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test
  fun firstLaunchHasNoPreferencesAndNeedsOnboarding() = runTest {
    val fixture = openStore()
    try {
      assertEquals(MovieTasteProfile(), fixture.repository.profile.first())
      assertTrue(fixture.repository.profile.first().needsOnboarding)
    } finally {
      fixture.job.cancelAndJoin()
    }
  }

  @Test
  fun completedProfileSurvivesClosingAndReopeningTheStore() = runTest {
    val file = storeFile()
    val preferences = MovieTastePreferences(
      setOf(28, 878), setOf(603, 550), 7,
      favouriteTitles = mapOf(603 to "The Matrix", 550 to "Fight Club: café edition")
    )
    val original = openStore(file)
    try {
      original.repository.savePreferences(preferences)
    } finally {
      original.job.cancelAndJoin()
    }

    val reopened = openStore(file)
    try {
      assertEquals(
        MovieTasteProfile(preferences, OnboardingStatus.COMPLETED),
        reopened.repository.profile.first()
      )
      assertFalse(reopened.repository.profile.first().needsOnboarding)
    } finally {
      reopened.job.cancelAndJoin()
    }
  }

  @Test
  fun skippingSurvivesReopeningAndDoesNotCreatePreferences() = runTest {
    val file = storeFile()
    val original = openStore(file)
    try {
      original.repository.skipOnboarding()
    } finally {
      original.job.cancelAndJoin()
    }

    val reopened = openStore(file)
    try {
      assertEquals(
        MovieTasteProfile(onboardingStatus = OnboardingStatus.SKIPPED),
        reopened.repository.profile.first()
      )
      assertFalse(reopened.repository.profile.first().needsOnboarding)
    } finally {
      reopened.job.cancelAndJoin()
    }
  }

  @Test
  fun editingReplacesSelectionsAndCanRemoveTheRatingLimit() = runTest {
    val fixture = openStore()
    try {
      fixture.repository.savePreferences(MovieTastePreferences(
        setOf(28, 35), setOf(603), 8, favouriteTitles = mapOf(603 to "The Matrix")
      ))
      val edited = MovieTastePreferences(
        setOf(18), setOf(550), null, favouriteTitles = mapOf(550 to "Fight Club")
      )
      fixture.repository.savePreferences(edited)
      assertEquals(
        MovieTasteProfile(edited, OnboardingStatus.COMPLETED),
        fixture.repository.profile.first()
      )
    } finally {
      fixture.job.cancelAndJoin()
    }
  }

  @Test
  fun lateSkipCannotOverwriteCompletedPreferences() = runTest {
    val fixture = openStore()
    try {
      val preferences = MovieTastePreferences(setOf(99), setOf(603), 6)
      fixture.repository.savePreferences(preferences)
      fixture.repository.skipOnboarding()
      assertEquals(
        MovieTasteProfile(preferences, OnboardingStatus.COMPLETED),
        fixture.repository.profile.first()
      )
    } finally {
      fixture.job.cancelAndJoin()
    }
  }

  @Test
  fun preferencesCanBeAddedAfterSkipping() = runTest {
    val fixture = openStore()
    try {
      fixture.repository.skipOnboarding()
      val preferences = MovieTastePreferences(favouriteMovieIds = setOf(603))
      fixture.repository.savePreferences(preferences)
      assertEquals(
        MovieTasteProfile(preferences, OnboardingStatus.COMPLETED),
        fixture.repository.profile.first()
      )
    } finally {
      fixture.job.cancelAndJoin()
    }
  }

  @Test
  fun genreOnlyMovieOnlyAndEmptyAnswersCanCompleteOnboarding() = runTest {
    val fixture = openStore()
    try {
      for (preferences in listOf(
        MovieTastePreferences(genreIds = setOf(28)),
        MovieTastePreferences(favouriteMovieIds = setOf(603)),
        MovieTastePreferences()
      )) {
        fixture.repository.savePreferences(preferences)
        assertEquals(
          MovieTasteProfile(preferences, OnboardingStatus.COMPLETED),
          fixture.repository.profile.first()
        )
      }
    } finally {
      fixture.job.cancelAndJoin()
    }
  }

  @Test
  fun existingObserverReceivesTheCompleteSavedProfile() = runTest {
    val fixture = openStore()
    try {
      assertTrue(fixture.repository.profile.first().needsOnboarding)
      val update = async {
        fixture.repository.profile.first { !it.needsOnboarding }
      }
      val preferences = MovieTastePreferences(setOf(28, 18), setOf(603), 7)
      fixture.repository.savePreferences(preferences)
      assertEquals(MovieTasteProfile(preferences, OnboardingStatus.COMPLETED), update.await())
    } finally {
      fixture.job.cancelAndJoin()
    }
  }

  @Test
  fun invalidStoredIdsAreReportedInsteadOfResettingOnboarding() = runTest {
    val fixture = openStore()
    try {
      fixture.store.edit {
        it[stringSetPreferencesKey("genre_ids")] = setOf("not-an-id")
      }
      try {
        fixture.repository.profile.first()
        fail("Expected corrupt preferences to be reported")
      } catch (_: CorruptionException) {
        // The UI can offer retry or continue; corrupted data is not treated as a new installation.
      }
      assertEquals(
        setOf("not-an-id"),
        fixture.store.data.first()[stringSetPreferencesKey("genre_ids")]
      )
    } finally {
      fixture.job.cancelAndJoin()
    }
  }

  @Test
  fun legacyIdsWithoutTitlesKeepTheCompletedProfileReadable() = runTest {
    val fixture = openStore()
    try {
      fixture.store.edit {
        it[stringSetPreferencesKey("favourite_movie_ids")] = setOf("603", "550")
        it[stringPreferencesKey("onboarding_status")] = "COMPLETED"
      }
      val profile = fixture.repository.profile.first()
      assertEquals(setOf(603, 550), profile.preferences.favouriteMovieIds)
      assertTrue(profile.preferences.favouriteTitles.isEmpty())
      assertFalse(profile.needsOnboarding)
    } finally {
      fixture.job.cancelAndJoin()
    }
  }

  @Test
  fun invalidOptionalTitlesDoNotDiscardFavouriteIdsOrOnboardingStatus() = runTest {
    val fixture = openStore()
    try {
      fixture.repository.savePreferences(MovieTastePreferences(
        favouriteMovieIds = setOf(603), favouriteTitles = mapOf(603 to "The Matrix")
      ))
      for (encoded in listOf("{broken", """{"603":"  ","999":"Not selected"}""")) {
        fixture.store.edit { it[stringPreferencesKey("favourite_titles")] = encoded }
        val profile = fixture.repository.profile.first()
        assertEquals(setOf(603), profile.preferences.favouriteMovieIds)
        assertTrue(profile.preferences.favouriteTitles.isEmpty())
        assertFalse(profile.needsOnboarding)
      }
    } finally {
      fixture.job.cancelAndJoin()
    }
  }

  @Test
  fun invalidQuizValuesAreRejected() {
    for (invalid in listOf<() -> MovieTastePreferences>(
      { MovieTastePreferences(genreIds = setOf(1, 2, 3, 4, 5, 6)) },
      { MovieTastePreferences(genreIds = setOf(0)) },
      { MovieTastePreferences(favouriteMovieIds = setOf(-1)) },
      { MovieTastePreferences(minimumRating = 5) },
      { MovieTastePreferences(minimumRating = 9) },
      { MovieTastePreferences(favouriteMovieIds = setOf(603), favouriteTitles = mapOf(999 to "Other")) },
      { MovieTastePreferences(favouriteMovieIds = setOf(603), favouriteTitles = mapOf(603 to " ")) }
    )) {
      try {
        invalid()
        fail("Expected invalid quiz preferences to be rejected")
      } catch (_: IllegalArgumentException) {
        // Reject values outside the quiz contract before writing anything.
      }
    }
  }

  private fun storeFile() = File(temporaryFolder.newFolder(), "taste.preferences_pb")

  private fun TestScope.openStore(file: File = storeFile()): Fixture {
    val job = SupervisorJob()
    // Android's FileStorage sees SDK_INT = 0 in plain JVM tests and falls back to
    // File.renameTo, which cannot replace existing files on Windows. Keep real
    // persistence here, using DataStore's host-compatible Okio storage.
    val store = PreferenceDataStoreFactory.create(
      storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { file.toOkioPath() },
      scope = CoroutineScope(job + StandardTestDispatcher(testScheduler))
    )
    return Fixture(store, DataStoreMovieTasteRepository(store), job)
  }

  private data class Fixture(
    val store: DataStore<Preferences>,
    val repository: MovieTasteRepository,
    val job: Job
  )
}
