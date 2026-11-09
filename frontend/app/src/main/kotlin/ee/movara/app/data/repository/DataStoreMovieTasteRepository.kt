package ee.movara.app.data.repository

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import ee.movara.app.domain.model.MovieTastePreferences
import ee.movara.app.domain.model.MovieTasteProfile
import ee.movara.app.domain.model.OnboardingStatus
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class DataStoreMovieTasteRepository(
  private val dataStore: DataStore<Preferences>
) : MovieTasteRepository {
  override val profile = dataStore.data.map { it.toProfile() }.distinctUntilChanged()

  override suspend fun savePreferences(preferences: MovieTastePreferences) {
    dataStore.edit { stored ->
      stored[GENRE_IDS] = preferences.genreIds.map(Int::toString).toSet()
      stored[FAVOURITE_MOVIE_IDS] = preferences.favouriteMovieIds.map(Int::toString).toSet()
      stored[FAVOURITE_TITLES] = Json.encodeToString(preferences.favouriteTitles)
      val minimumRating = preferences.minimumRating
      if (minimumRating == null) {
        stored.remove(MINIMUM_RATING)
      } else {
        stored[MINIMUM_RATING] = minimumRating
      }
      stored[ONBOARDING_STATUS] = OnboardingStatus.COMPLETED.name
    }
  }

  override suspend fun skipOnboarding() {
    dataStore.edit { stored ->
      if (stored.toProfile().needsOnboarding) {
        stored[ONBOARDING_STATUS] = OnboardingStatus.SKIPPED.name
      }
    }
  }

  private fun Preferences.toProfile(): MovieTasteProfile = try {
    val favouriteIds = this[FAVOURITE_MOVIE_IDS].orEmpty().map(String::toInt).toSet()
    MovieTasteProfile(
      preferences = MovieTastePreferences(
        genreIds = this[GENRE_IDS].orEmpty().map(String::toInt).toSet(),
        favouriteMovieIds = favouriteIds,
        minimumRating = this[MINIMUM_RATING],
        favouriteTitles = readFavouriteTitles(favouriteIds)
      ),
      onboardingStatus = this[ONBOARDING_STATUS]?.let(OnboardingStatus::valueOf)
        ?: OnboardingStatus.NOT_STARTED
    )
  } catch (exception: IllegalArgumentException) {
    // Never turn unreadable preferences into a successful, empty first-launch profile.
    throw CorruptionException("Cannot read movie taste preferences", exception)
  }

  private fun Preferences.readFavouriteTitles(ids: Set<Int>): Map<Int, String> {
    val encoded = this[FAVOURITE_TITLES] ?: return emptyMap()
    return try {
      Json.decodeFromString<Map<Int, String>>(encoded)
        .filter { (id, title) -> id in ids && title.isNotBlank() }
    } catch (_: IllegalArgumentException) {
      // Optional display metadata must not make an otherwise valid legacy profile unreadable.
      emptyMap()
    }
  }

  private companion object {
    val GENRE_IDS = stringSetPreferencesKey("genre_ids")
    val FAVOURITE_MOVIE_IDS = stringSetPreferencesKey("favourite_movie_ids")
    val FAVOURITE_TITLES = stringPreferencesKey("favourite_titles")
    val MINIMUM_RATING = intPreferencesKey("minimum_rating")
    val ONBOARDING_STATUS = stringPreferencesKey("onboarding_status")
  }
}
