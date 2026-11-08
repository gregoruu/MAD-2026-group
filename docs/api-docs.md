# TMDB API layer

Movara reads all movie data from [TMDB](https://developer.themoviedb.org/docs). The app does
not call TMDB directly: every request goes to the TMDB proxy in `backend/`, a Cloudflare
Worker that adds the API key and forwards the request to `https://api.themoviedb.org/3`.
The app code lives in `frontend/app/src/main/kotlin/ee/movara/app/data/remote`, the proxy
in `backend/src/index.ts`.

## The client

| Piece | File | What it is |
| --- | --- | --- |
| Base URL | `frontend/app/build.gradle.kts` | `BuildConfig.TMDB_BASE_URL`, the proxy for the build type (see below) |
| HTTP client | `TmdbNetwork.kt` | OkHttp, 10 s connect timeout, 20 s read timeout |
| Endpoints | `TmdbApiService.kt` | Retrofit interface, one `suspend` function per endpoint |
| Parsing | `TmdbJson.kt` | kotlinx.serialization, `ignoreUnknownKeys` and `coerceInputValues` on |
| Wiring | `AppContainer.kt` | One shared client and one service for the whole app |

| Build type | Base URL |
| --- | --- |
| `debug`, `staging` | `https://movara-tmdb-proxy-staging.movara.workers.dev/` |
| `release` | `https://movara-tmdb-proxy-production.movara.workers.dev/` |

The app holds **no API key**. Requests leave the phone without one, and the proxy adds the
key from a Cloudflare secret. No build of the app contains the key, and developers do not
need a key of their own.

Network access requires the `INTERNET` permission, declared in `AndroidManifest.xml`.

## The proxy

The proxy forwards only the five endpoints below, each with only the query parameters and
values the app sends: `query` is not blank and at most 100 characters, `page` is 1 to 500,
`sort_by` is only `popularity.desc`, `with_genres` is one genre id and `append_to_response`
is only `credits`. Any other request is rejected before it reaches TMDB. The full rules,
deployment and secret handling are in `backend/README.md`.

A new endpoint, parameter or value therefore needs two changes: the function on
`TmdbApiService`, and the rule in `ENDPOINTS` in `backend/src/index.ts`. The proxy has to be
deployed with the new rule before an app build that uses it, otherwise the proxy answers
`400`.

## Endpoints

All paths are relative to the base URL. A page holds 20 movies and the response says how
many pages exist.

| Function | Request | Used for |
| --- | --- | --- |
| `searchMovies(title, page)` | `GET search/movie?query=&page=` | Search |
| `trendingMovies(page)` | `GET trending/movie/week?page=` | Home, when nothing is liked yet |
| `recommendedMovies(movieId, page)` | `GET movie/{id}/recommendations?page=` | Home rails "Because you liked …" |
| `moviesInGenre(genreId, page)` | `GET discover/movie?with_genres=&sort_by=popularity.desc&page=` | A selected genre |
| `movieDetails(movieId)` | `GET movie/{id}?append_to_response=credits` | Movie Details |

`search/movie`, `trending/movie/week`, `recommendations` and `discover/movie` all answer
with the same shape and are parsed into `MoviePageDto`.

Movie Details asks for credits in the same request with `append_to_response=credits`, so one
screen costs one request. The cast and crew arrive in `MovieDetailsDto.credits`.

The ten genres on the Genres screen are fixed in the app (see section 9 of
`design-specs.md`), so there is no request for the genre list.

Images are not fetched through this service. Posters are `https://image.tmdb.org/t/p/{size}{path}`
and are loaded by the image loader from the paths in the responses.

## Results and failures

Every function suspends and returns a parsed response. A request that fails throws:
`retrofit2.HttpException` for a non-2xx answer, `java.io.IOException` when the phone is
offline or the proxy does not answer, and `kotlinx.serialization.SerializationException`
for a response that cannot be parsed.

| Status | Comes from | Means |
| --- | --- | --- |
| `400` | Proxy | The request breaks a proxy rule; the app and the proxy rules are out of step |
| `401` | TMDB | The key in the proxy's secret is wrong or revoked |
| `404` | TMDB or proxy | An unknown movie, or a path the proxy does not forward |
| `500` | Proxy | The proxy has no `TMDB_API_KEY` secret |
| `502` | Proxy | The proxy could not reach TMDB |

TMDB's own error answers pass through the proxy unchanged.

Callers above the data layer use `MovieRepository` and never see those exceptions.
`RepositoryResult.Error` contains a safe message, category (invalid input, network,
HTTP, invalid response, database or unexpected failure) and optional HTTP status.
Coroutine cancellation is rethrown. Blank search queries and non-positive IDs/pages
return an input error without calling TMDB.

Successful results contain domain models, pagination metadata for lists, a source
(`NETWORK` / `CACHE` / `LOCAL`) and, for TMDB data, the last successful fetch timestamp.
Each request attempts TMDB once. Validated successful DTOs are serialized into Room's
`tmdb_cache` table before returning fresh data. A storage write failure returns usable
network data with `cachePersisted = false`.

Offline/timeouts, HTTP 408/429/5xx, serialization failures and unusable responses try
the matching persistent cache entry. Valid cached data returns `Success(source = CACHE)`;
its timestamp remains unchanged, so the UI can identify stale content. Missing or corrupt
cache data returns an error. Other HTTP failures, such as 401/403/404, remain errors.
Failed requests do not overwrite successful entries. There is no automatic retry,
expiry or eviction.

Cache keys distinguish endpoint, query/IDs and page, for example `search:interstellar:1`,
`trending:1`, `recommendations:603:1`, `genre:28:2` and `details:603`.
Search queries are trimmed, whitespace collapsed and lowercased with Locale.ROOT for
both the key and request. Cached responses survive app restarts, independently of
the user's saved lists. Images are outside this cache (#48).

Responses with missing optional fields use the existing `TmdbMappers.kt` behavior.
Unusable cards are dropped; a wholly unusable nonempty page, invalid pagination,
or details without the requested ID and a usable title is treated as an invalid response.
An empty valid page is a successful response and can be cached.

## Logging

With `BuildConfig.ENABLE_HTTP_LOGGING` the client logs request and response bodies. The flag
is `true` for debug and staging and `false` for release, so a shipped build logs nothing.

The requests carry no API key, so the log contains none.

## Tests

`TmdbApiServiceTest` runs every endpoint against a local MockWebServer and checks the
request that leaves the client, the parsed response and that no request carries an
`api_key`.

`TmdbApiServiceLiveTest` calls the real TMDB API through the staging proxy and parses the
answer. It is the only app test that goes out to the network, so it is skipped unless the
run asks for it with `-PrunLiveTmdbTests` (or a `RUN_LIVE_TMDB_TESTS` environment variable
on a build server):

```
./gradlew :app:testDebugUnitTest -PrunLiveTmdbTests --tests "*TmdbApiServiceLiveTest"
```

A plain `./gradlew test` leaves it out, so the ordinary test run does not depend on the
network or on the proxy being up.

The proxy has its own tests in `backend/`. They replace `fetch` with a mock, so they never
call TMDB; run them with `npm test` in `backend/`.
