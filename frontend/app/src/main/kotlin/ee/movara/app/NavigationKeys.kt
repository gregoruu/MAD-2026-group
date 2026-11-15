
package ee.movara.app

import kotlinx.serialization.Serializable

// Main destinations
@Serializable data object HomeRoute
@Serializable data object GenresRoute
@Serializable data object MyMoviesRoute

// Secondary destinations
@Serializable data object SearchRoute
@Serializable data class SelectedGenreRoute(val genreId: Int)
@Serializable data class MovieDetailsRoute(val movieId: Int)
@Serializable data object AboutRoute
@Serializable data object MoviePreferencesRoute
