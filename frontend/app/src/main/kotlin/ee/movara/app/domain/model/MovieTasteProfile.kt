package ee.movara.app.domain.model

/** Explicit preferences, independent of the user's Like, Watch Later and Watched lists. */
data class MovieTastePreferences(
  val genreIds: Set<Int> = emptySet(),
  val favouriteMovieIds: Set<Int> = emptySet(),
  val minimumRating: Int? = null
) {
  init {
    require(genreIds.size <= MAX_GENRES) { "Select at most $MAX_GENRES genres" }
    require(genreIds.all { it > 0 }) { "Genre IDs must be positive" }
    require(favouriteMovieIds.all { it > 0 }) { "Movie IDs must be positive" }
    require(minimumRating == null || minimumRating in 6..8) {
      "Minimum TMDB rating must be absent, 6, 7 or 8"
    }
  }

  companion object {
    const val MAX_GENRES = 5
  }
}

/** Persisted separately from draft quiz answers. Only a successful save completes onboarding. */
data class MovieTasteProfile(
  val preferences: MovieTastePreferences = MovieTastePreferences(),
  val onboardingStatus: OnboardingStatus = OnboardingStatus.NOT_STARTED
) {
  val needsOnboarding: Boolean get() = onboardingStatus == OnboardingStatus.NOT_STARTED
}

enum class OnboardingStatus {
  NOT_STARTED,
  COMPLETED,
  SKIPPED
}
