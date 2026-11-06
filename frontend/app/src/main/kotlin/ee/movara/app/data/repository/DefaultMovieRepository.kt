package ee.movara.app.data.repository

import ee.movara.app.data.local.MovieListType
import ee.movara.app.data.local.SavedMovieDao
import ee.movara.app.data.local.SavedMovieEntity
import ee.movara.app.data.local.TmdbCacheDao
import ee.movara.app.data.local.TmdbCacheEntity
import ee.movara.app.data.remote.MovieDetailsDto
import ee.movara.app.data.remote.MoviePageDto
import ee.movara.app.data.remote.TmdbApiService
import ee.movara.app.data.remote.tmdbJson
import ee.movara.app.data.remote.toDomainMovies
import ee.movara.app.data.remote.toDomainOrNull
import ee.movara.app.domain.model.Movie
import ee.movara.app.domain.model.MovieDetails
import ee.movara.app.domain.model.MoviePage
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

class DefaultMovieRepository(
  private val api: TmdbApiService,
  private val cache: TmdbCacheDao,
  private val savedMovies: SavedMovieDao,
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val now: () -> Long = System::currentTimeMillis
) : MovieRepository {
  override suspend fun searchMovies(title: String, page: Int): RepositoryResult<MoviePage> {
    val query = title.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    if (query.isBlank() || page <= 0) return invalidInput()
    return moviePage("search:$query:$page", page) { api.searchMovies(query, page) }
  }

  override suspend fun trendingMovies(page: Int): RepositoryResult<MoviePage> {
    if (page <= 0) return invalidInput()
    return moviePage("trending:$page", page) { api.trendingMovies(page) }
  }

  override suspend fun recommendedMovies(movieId: Int, page: Int): RepositoryResult<MoviePage> {
    if (movieId <= 0 || page <= 0) return invalidInput()
    return moviePage("recommendations:$movieId:$page", page) {
      api.recommendedMovies(movieId, page)
    }
  }

  override suspend fun moviesInGenre(genreId: Int, page: Int): RepositoryResult<MoviePage> {
    if (genreId <= 0 || page <= 0) return invalidInput()
    return moviePage("genre:$genreId:$page", page) { api.moviesInGenre(genreId, page) }
  }

  override suspend fun movieDetails(movieId: Int): RepositoryResult<MovieDetails> {
    if (movieId <= 0) return invalidInput()
    return request<MovieDetailsDto, MovieDetails>(
      "details:$movieId", { api.movieDetails(movieId) }
    ) { dto ->
      require(dto.id == movieId) { "Mismatched movie ID" }
      requireNotNull(dto.toDomainOrNull()) { "Movie has no usable ID or title" }
    }
  }

  private suspend fun moviePage(
    key: String,
    page: Int,
    fetch: suspend () -> MoviePageDto
  ): RepositoryResult<MoviePage> = request<MoviePageDto, MoviePage>(key, fetch) { dto ->
    require(dto.page == page && dto.totalPages >= 0 && dto.totalResults >= 0)
    require(dto.totalPages == 0 || dto.page <= dto.totalPages)
    val movies = dto.toDomainMovies()
    // Preserve the mapper's per-card tolerance, but reject a wholly unusable response.
    require(dto.results.isEmpty() || movies.isNotEmpty())
    require(dto.totalPages > 0 || (dto.results.isEmpty() && dto.totalResults == 0))
    MoviePage(movies, dto.page, dto.totalPages, dto.totalResults)
  }

  /** One network attempt. Only validated successful responses can replace a cache entry. */
  private suspend inline fun <reified D, T> request(
    key: String,
    crossinline fetch: suspend () -> D,
    crossinline map: (D) -> T
  ): RepositoryResult<T> = withContext(dispatcher) {
    val dto: D
    val data: T
    try {
      dto = fetch()
      data = map(dto)
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (failure: Exception) {
      val error = remoteError(failure)
      val recoverable = failure is IOException || failure is SerializationException ||
        failure is IllegalArgumentException ||
        (failure is HttpException && (failure.code() in 500..599 ||
          failure.code() == 408 || failure.code() == 429))
      if (!recoverable) return@withContext error
      return@withContext try {
        val entry = cache.get(key) ?: return@withContext error
        val cached = map(tmdbJson.decodeFromString<D>(entry.responseJson))
        RepositoryResult.Success(cached, DataSource.CACHE, entry.fetchedAt, cachePersisted = true)
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: SerializationException) {
        error
      } catch (_: IllegalArgumentException) {
        error
      } catch (_: Exception) {
        databaseError()
      }
    }

    val fetchedAt = now()
    val persisted = try {
      cache.upsert(TmdbCacheEntity(key, tmdbJson.encodeToString(dto), fetchedAt))
      true
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      // A storage failure must not discard usable fresh data or masquerade as offline data.
      false
    }
    RepositoryResult.Success(data, DataSource.NETWORK, fetchedAt, persisted)
  }

  override fun observeMovies(listType: MovieListType): Flow<RepositoryResult<List<Movie>>> =
    observeLocal {
      savedMovies.observeMovies(listType).map { entries ->
        entries.map { Movie(it.tmdbId, it.title, it.posterPath, it.year) }
      }
    }

  override fun observeCount(listType: MovieListType): Flow<RepositoryResult<Int>> =
    observeLocal { savedMovies.observeCount(listType) }

  override fun observeMembership(
    movieId: Int,
    listType: MovieListType
  ): Flow<RepositoryResult<Boolean>> =
    if (movieId <= 0) flowOf(invalidInput())
    else observeLocal { savedMovies.observeMembership(movieId, listType) }

  private fun <T> observeLocal(source: () -> Flow<T>): Flow<RepositoryResult<T>> =
    flow { emitAll(source()) }
      .map<T, RepositoryResult<T>> { RepositoryResult.Success(it, DataSource.LOCAL) }
      .catch { failure ->
        if (failure is CancellationException || failure !is Exception) throw failure
        emit(databaseError())
      }
      .flowOn(dispatcher)

  override suspend fun saveMovie(movie: Movie, listType: MovieListType): RepositoryResult<Unit> {
    if (movie.id <= 0 || movie.title.isBlank()) return invalidInput()
    return writeLocal {
      savedMovies.upsert(
        SavedMovieEntity(
          movie.id, movie.title, movie.posterPath, movie.releaseYear, listType, now()
        )
      )
    }
  }

  override suspend fun removeMovie(movieId: Int, listType: MovieListType): RepositoryResult<Unit> {
    if (movieId <= 0) return invalidInput()
    return writeLocal { savedMovies.remove(movieId, listType) }
  }

  private suspend fun writeLocal(write: suspend () -> Unit): RepositoryResult<Unit> =
    withContext(dispatcher) {
      try {
        write()
        RepositoryResult.Success(Unit, DataSource.LOCAL)
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        databaseError()
      }
    }

  private fun invalidInput() = RepositoryResult.Error(
    ErrorCategory.INVALID_INPUT, "Provide a nonblank title and positive IDs and page numbers."
  )

  private fun databaseError() = RepositoryResult.Error(
    ErrorCategory.DATABASE, "Movie storage is unavailable."
  )

  private fun remoteError(failure: Exception): RepositoryResult.Error = when (failure) {
    is IOException -> RepositoryResult.Error(
      ErrorCategory.NETWORK, "Unable to reach TMDB. Check your connection or try again later."
    )
    is HttpException -> RepositoryResult.Error(
      ErrorCategory.HTTP, "TMDB could not complete the request.", failure.code()
    )
    is SerializationException, is IllegalArgumentException -> RepositoryResult.Error(
      ErrorCategory.INVALID_RESPONSE, "TMDB returned an unusable response."
    )
    else -> RepositoryResult.Error(ErrorCategory.UNEXPECTED, "The movie request failed.")
  }
}
