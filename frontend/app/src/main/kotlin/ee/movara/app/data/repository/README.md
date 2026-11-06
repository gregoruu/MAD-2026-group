# Movie repository

Inject `AppContainer.movieRepository` into movie ViewModel constructors as
`MovieRepository`. It is the shared boundary for TMDB and Room; callers receive
domain models and never use Retrofit or DAOs directly.

- `searchMovies(title, page)`, `trendingMovies(page)`,
  `recommendedMovies(movieId, page)`, `moviesInGenre(genreId, page)` return
  `RepositoryResult<MoviePage>`, preserving page, total pages and total results.
- `movieDetails(movieId)` returns `RepositoryResult<MovieDetails>`.
- `observeLikedMovies()`, `observeWatchLaterMovies()`, `observeWatchedMovies()`
  and `observeMovies(listType)` expose flows of results containing domain movies.
  `observeCount` and `observeMembership` expose reactive counts and membership.
- `saveMovie` upserts one list entry; `removeMovie` removes only that membership.
  Saving `WATCHED` inserts/updates it and removes `WATCH_LATER` in one Room
  transaction. Liked membership is independent. Removing watched does not restore
  Watch Later. Watch Later can explicitly be added again later.
- `observeLists()` maps one Room query into a consistent `MovieLists` snapshot.
  The navigation-scoped `MovieListsViewModel` shares it across Home, My Movies and
  Movie Details. Buttons stay disabled while loading, after storage errors and
  while a write for that movie is pending; failures never fake a changed selection.
  The observed Room snapshot confirms each successful write before re-enabling actions.
- Home filters watched IDs and duplicate movies from its trending candidates and
  recomputes on list changes without another network request. The same filtering
  must be retained when personal recommendation selection (#40) replaces trending.
  Raw TMDB endpoints and their cached responses are not filtered or rewritten.

The navigation screens provide the list-action integration for #39. They are not
the complete screen designs for #32, #36 and #37. Search and Genres remain separate
features, and undo snackbars belong to #44. Unlike, remove Watch Later and unmark
Watched are supported directly by the toggled controls.

`RepositoryResult.Success` carries `NETWORK`, `CACHE` or `LOCAL` source.
TMDB successes include the last successful fetch time in epoch milliseconds.
`CACHE` means fallback content that may be stale. `Error` carries a category,
safe message and optional HTTP status; no low-level exception. Cancellation is rethrown.
An observation failure emits a database error and ends that collection; collect again
to retry. Invalid titles, IDs and pages return an input error without a request.

`DefaultMovieRepository` makes one network attempt, validates/maps the response,
and stores the existing serializable DTO in the separate Room `tmdb_cache` table.
Keys include endpoint, IDs/query and page. Search queries are trimmed, whitespace
collapsed and lowercased using Locale.ROOT for both the request and key.
Valid empty responses are cached; unusable cards are dropped by the existing mapper,
but a wholly unusable nonempty page or invalid details/pagination is a failure.

Offline/timeouts, HTTP 408/429/5xx and malformed responses fall back to the same
validated cached response. Other HTTP failures (including 401/403/404) remain errors.
Failed requests never overwrite cache entries. There is no TTL, eviction or automatic
retry; each call attempts fresh data, and fallback retains the previous fetch time.
If a cache write fails, fresh data is returned with `cachePersisted = false`.
Cache data and saved-list data are independent. Saved entries use `SavedMovieDao`.

Database v2 adds the cache via `MovaraDatabase.MIGRATION_1_2`; v1 saved lists and
their index remain untouched. Both schemas are exported under `app/schemas`.

## Tests

The existing setup uses JUnit4, coroutines-test, manual constructor injection and
Compose/Espresso instrumentation; no mocking or DI framework is needed here.
JVM repository tests inject a fake TMDB service and fake DAOs to test mapping,
keys, failures, invalid input and cancellation. Instrumented tests inject the same
fake API with real in-memory Room, plus file-backed reopening and migration tests.
The migration test creates v1 from the committed schema asset and lets Room validate v2.
Tests need neither a TMDB key nor external network access.

From the repository root:

```
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

The starter `DataRepository` remains solely for the greeting example.
