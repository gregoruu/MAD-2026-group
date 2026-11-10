package ee.movara.app.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.domain.model.Movie

/** Keeps the shared list coordinator alive for the navigation lifecycle. */
class MovieListsViewModel(repository: MovieRepository) : ViewModel() {
  val store = MovieListsStore(repository, viewModelScope)
  val uiState = store.uiState
  val feedback = store.feedback

  fun retry() = store.retry()
  fun setMembership(movie: Movie, type: MovieListType, selected: Boolean) =
    store.setMembership(movie, type, selected)
}
