package ee.movara.app.data.repository

import ee.movara.app.domain.model.MovieTastePreferences
import ee.movara.app.domain.model.MovieTasteProfile
import kotlinx.coroutines.flow.Flow

/** Local taste profile shared by the quiz and Home recommendations. */
interface MovieTasteRepository {
  /** Emits only persisted state; read errors propagate so the UI can offer retry or continue. */
  val profile: Flow<MovieTasteProfile>

  /** Atomically replaces preferences and completes onboarding. Also used when editing later. */
  suspend fun savePreferences(preferences: MovieTastePreferences)

  /** Dismisses first-launch onboarding without overwriting previously saved preferences. */
  suspend fun skipOnboarding()
}
