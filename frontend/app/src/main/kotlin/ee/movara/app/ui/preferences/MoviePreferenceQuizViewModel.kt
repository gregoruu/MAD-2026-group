package ee.movara.app.ui.preferences

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.movara.app.data.repository.MovieRepository
import ee.movara.app.data.repository.MovieTasteRepository
import ee.movara.app.data.repository.RepositoryResult
import ee.movara.app.domain.model.GenreCatalog
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieTastePreferences
import ee.movara.app.domain.model.MovieDiscovery
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class QuizStep { GENRES, MOVIES, RATING }

data class MoviePreferenceQuizState(
  val loadingProfile: Boolean = true,
  val profileLoadFailed: Boolean = false,
  val step: QuizStep = QuizStep.GENRES,
  val genreIds: Set<Int> = emptySet(),
  val favouriteMovieIds: Set<Int> = emptySet(),
  val favouriteTitles: Map<Int, String> = emptyMap(),
  val minimumRating: Int? = null,
  val query: String = "",
  val movies: List<Movie> = emptyList(),
  val loadingMovies: Boolean = false,
  val movieLoadFailed: Boolean = false,
  val page: Int = 0,
  val totalPages: Int = 0,
  val saving: Boolean = false,
  val saveFailed: Boolean = false,
  val finished: Boolean = false
) {
  val canLoadMore: Boolean get() = page > 0 && page < totalPages && page < 500
}

class MoviePreferenceQuizViewModel(
  private val tasteRepository: MovieTasteRepository,
  private val movieRepository: MovieRepository,
  private val savedState: SavedStateHandle,
  today: () -> LocalDate = { LocalDate.now(ZoneOffset.UTC) }
) : ViewModel() {
  private val state = MutableStateFlow(MoviePreferenceQuizState(
    step = QuizStep.entries[savedState.get<Int>("step") ?: 0],
    genreIds = savedState.get<IntArray>("genres")?.toSet().orEmpty(),
    favouriteMovieIds = savedState.get<IntArray>("favourites")?.toSet().orEmpty(),
    favouriteTitles = (savedState.get<IntArray>("titleIds")?.toList().orEmpty())
      .zip(savedState.get<ArrayList<String>>("titles").orEmpty()).toMap(),
    minimumRating = savedState.get<Int>("rating")?.takeIf { it != 0 },
    query = savedState["query"] ?: "",
    finished = savedState["finished"] ?: false
  ))
  val uiState = state.asStateFlow()
  private var movieJob: Job? = null
  private var profileJob: Job? = null
  private var requestedPage = 1
  private var loadedGenres: Set<Int>? = null
  private val sourcePages = mutableMapOf<Int, Int>()
  private val releaseCutoff = today()
  private val familiarOptions = MovieDiscovery.familiar(null, releaseCutoff)

  init { retryProfile() }

  fun retryProfile() {
    if (state.value.saving) return
    profileJob?.cancel()
    state.update { it.copy(loadingProfile = true, profileLoadFailed = false) }
    profileJob = viewModelScope.launch {
      try {
        val profile = tasteRepository.profile.first()
        if (savedState.get<Boolean>("initialized") != true) {
          state.update { it.copy(
            genreIds = profile.preferences.genreIds,
            favouriteMovieIds = profile.preferences.favouriteMovieIds,
            favouriteTitles = profile.preferences.favouriteTitles,
            minimumRating = profile.preferences.minimumRating
          ) }
        }
        state.update { it.copy(loadingProfile = false) }
        rememberDraft()
        loadFavouriteTitles()
        if (state.value.step == QuizStep.MOVIES) requestMovies(1)
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        state.update { it.copy(loadingProfile = false, profileLoadFailed = true) }
      }
    }
  }

  fun toggleGenre(id: Int) {
    if (!canEdit() || GenreCatalog.genres.none { it.id == id }) return
    val genres = state.value.genreIds
    if (id !in genres && genres.size >= MovieTastePreferences.MAX_GENRES) return
    state.update { it.copy(genreIds = if (id in genres) genres - id else genres + id) }
    rememberDraft()
  }

  fun clearGenres() {
    if (!canEdit()) return
    state.update { it.copy(genreIds = emptySet()) }
    rememberDraft()
  }

  fun toggleMovie(movie: Movie) {
    if (!canEdit()) return
    if (movie.id in state.value.favouriteMovieIds) {
      removeFavourite(movie.id)
    } else {
      state.update { it.copy(
        favouriteMovieIds = it.favouriteMovieIds + movie.id,
        favouriteTitles = it.favouriteTitles + (movie.id to movie.title)
      ) }
      rememberDraft()
    }
  }

  fun removeFavourite(id: Int) {
    if (!canEdit()) return
    state.update { it.copy(
      favouriteMovieIds = it.favouriteMovieIds - id,
      favouriteTitles = it.favouriteTitles - id
    ) }
    rememberDraft()
  }

  fun clearFavourites() {
    if (!canEdit()) return
    state.update { it.copy(favouriteMovieIds = emptySet(), favouriteTitles = emptyMap()) }
    rememberDraft()
  }

  fun selectRating(rating: Int?) {
    if (!canEdit() || (rating != null && rating !in 6..8)) return
    state.update { it.copy(minimumRating = rating) }
    rememberDraft()
  }

  fun next() {
    if (!canEdit()) return
    when (state.value.step) {
      QuizStep.GENRES -> {
        state.update { it.copy(step = QuizStep.MOVIES) }
        if (loadedGenres != state.value.genreIds || state.value.page == 0) requestMovies(1)
      }
      QuizStep.MOVIES -> {
        movieJob?.cancel()
        state.update { it.copy(step = QuizStep.RATING, loadingMovies = false) }
      }
      QuizStep.RATING -> return
    }
    rememberDraft()
  }

  fun previous() {
    if (!canEdit()) return
    movieJob?.cancel()
    state.update { it.copy(
      step = if (it.step == QuizStep.RATING) QuizStep.MOVIES else QuizStep.GENRES,
      loadingMovies = false
    ) }
    rememberDraft()
    if (state.value.step == QuizStep.MOVIES && state.value.page == 0) requestMovies(1)
  }

  fun onQueryChange(query: String) {
    if (!canEdit()) return
    val limited = query.take(100)
    if (limited == state.value.query) return
    state.update { it.copy(
      query = limited, movies = emptyList(), page = 0, totalPages = 0, movieLoadFailed = false
    ) }
    rememberDraft()
    requestMovies(1, debounce = limited.isNotBlank())
  }

  fun submitSearch() {
    if (canEdit()) requestMovies(1)
  }

  fun loadMore() {
    val current = state.value
    if (canEdit() && current.canLoadMore && !current.loadingMovies && !current.movieLoadFailed) {
      requestMovies(current.page + 1)
    }
  }

  fun retryMovies() {
    if (canEdit() && !state.value.loadingMovies) requestMovies(requestedPage)
  }

  fun finish() {
    if (!canEdit()) return
    val current = state.value
    val preferences = MovieTastePreferences(
      genreIds = current.genreIds,
      favouriteMovieIds = current.favouriteMovieIds,
      minimumRating = current.minimumRating,
      favouriteTitles = current.favouriteTitles.filter {
        (id, title) -> id in current.favouriteMovieIds && title.isNotBlank()
      }
    )
    save { tasteRepository.savePreferences(preferences) }
  }

  fun skip() {
    if (state.value.saving || state.value.finished) return
    save { tasteRepository.skipOnboarding() }
  }

  private fun save(operation: suspend () -> Unit) {
    profileJob?.cancel()
    movieJob?.cancel()
    state.update { it.copy(saving = true, saveFailed = false, loadingMovies = false) }
    viewModelScope.launch {
      try {
        operation()
        savedState["finished"] = true
        state.update { it.copy(saving = false, finished = true) }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        state.update { it.copy(saving = false, saveFailed = true) }
      }
    }
  }

  private fun canEdit() = !state.value.loadingProfile && !state.value.profileLoadFailed &&
    !state.value.saving && !state.value.finished

  private fun rememberDraft() {
    val current = state.value
    savedState["initialized"] = true
    savedState["step"] = current.step.ordinal
    savedState["genres"] = current.genreIds.toIntArray()
    savedState["favourites"] = current.favouriteMovieIds.toIntArray()
    savedState["titleIds"] = current.favouriteTitles.keys.toIntArray()
    savedState["titles"] = ArrayList(current.favouriteTitles.values)
    savedState["rating"] = current.minimumRating ?: 0
    savedState["query"] = current.query
  }

  private fun loadFavouriteTitles() = viewModelScope.launch {
    val missing = state.value.favouriteMovieIds - state.value.favouriteTitles.keys
    val semaphore = Semaphore(3)
    val titles = coroutineScope {
      missing.map { id -> async {
        semaphore.withPermit {
          try {
            val result = movieRepository.movieDetails(id)
            if (result is RepositoryResult.Success) id to result.data.title else null
          } catch (cancelled: CancellationException) {
            throw cancelled
          } catch (_: Exception) { null }
        }
      } }.awaitAll().filterNotNull().toMap()
    }
    state.update { it.copy(
      favouriteTitles = it.favouriteTitles + titles.filterKeys(it.favouriteMovieIds::contains)
    ) }
    rememberDraft()
  }

  private fun requestMovies(page: Int, debounce: Boolean = false) {
    movieJob?.cancel()
    requestedPage = page
    val query = state.value.query.trim()
    val genres = state.value.genreIds.toSet()
    state.update { it.copy(loadingMovies = true, movieLoadFailed = false) }
    movieJob = viewModelScope.launch {
      if (debounce) delay(400)
      if (page == 1) sourcePages.clear()
      try {
        val results = if (query.isNotBlank()) {
          listOf(0 to movieRepository.searchMovies(query, page))
        } else {
          val sources = genres.ifEmpty { setOf(0) }
            .filter { page == 1 || page <= (sourcePages[it] ?: 500) }
          coroutineScope {
            sources.map { id -> async {
              id to movieRepository.discoverMovies(
                familiarOptions.copy(genreId = id.takeIf { it != 0 }), page
              )
            } }.awaitAll()
          }
        }
        currentCoroutineContext().ensureActive()
        val pages = results.mapNotNull { (id, result) ->
          if (result is RepositoryResult.Success) {
            sourcePages[id] = minOf(result.data.totalPages, 500)
            result.data
          } else null
        }
        // Interleave genres so the first genre cannot fill the entire initial selection.
        val movies = (0 until (pages.maxOfOrNull { it.movies.size } ?: 0)).flatMap { index ->
          pages.mapNotNull { it.movies.getOrNull(index) }
        }.distinctBy(Movie::id).filter { movie ->
          val released = movie.releaseDate?.let { !it.isAfter(releaseCutoff) } == true
          // Direct title search may find an enjoyed film below the browsing quality floor.
          released && (query.isNotBlank() ||
            ((movie.rating ?: 0.0) >= (familiarOptions.minimumRating ?: 0) &&
              (movie.voteCount ?: 0) >= (familiarOptions.minimumVotes ?: 0)))
        }
        val failed = results.any { it.second is RepositoryResult.Error }
        loadedGenres = genres
        state.update { it.copy(
          movies = ((if (page == 1) emptyList() else it.movies) + movies).distinctBy(Movie::id),
          loadingMovies = false,
          movieLoadFailed = failed,
          page = if (failed) it.page else page,
          totalPages = sourcePages.values.maxOrNull() ?: 0
        ) }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        state.update { it.copy(loadingMovies = false, movieLoadFailed = true) }
      }
    }
  }
}
