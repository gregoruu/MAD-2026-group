
package ee.movara.app

import kotlinx.serialization.Serializable

// Temporary starter screen
@Serializable data object Main

// Main destinations
@Serializable data object HomeRoute
@Serializable data object GenresRoute
@Serializable data object MyMoviesRoute

// Secondary destinations
@Serializable data object SearchRoute
@Serializable data class SelectedGenreRoute(val genreId: Int)
@Serializable data class MovieDetailsRoute(val movieId: Int)
@Serializable data object AboutRoute
