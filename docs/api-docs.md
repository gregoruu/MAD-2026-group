# TMDB API layer

Movara reads movie metadata from [TMDB](https://developer.themoviedb.org/docs). The app sends
all metadata requests to the TMDB proxy in `backend/`, a Cloudflare
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

The proxy forwards only the endpoints below, each with only the query parameters and
values the app sends: `query` is not blank and at most 100 characters; `page` is 1 to 500.
Discover accepts `sort_by` values `popularity.desc`, `vote_count.desc` and
`vote_average.desc`, an optional single `with_genres` ID, integer `vote_average.gte`
(0–10), integer `vote_count.gte` (0–2147483647), and a valid ISO date in
`primary_release_date.lte`. `append_to_response` is only `credits`. Any other request is rejected before it reaches TMDB. The full rules,
deployment and secret handling are in [backend/README.md](../backend/README.md).

A new endpoint, parameter or value therefore needs two changes: the function on
`TmdbApiService`, and the rule in `ENDPOINTS` in `backend/src/index.ts`. The proxy has to be
deployed with the new rule before an app build that uses it; an unsupported parameter or
value produces `400`, while an unsupported path produces `404`.

## Endpoints

All paths are relative to the Worker's base URL (without a `/3` prefix). List responses
contain up to 20 movies per page and report pagination totals.

| Function | Request | Used for |
| --- | --- | --- |
| `searchMovies(title, page)` | `GET /search/movie?query={title}&page={page}` | Search and quiz title search |
| `trendingMovies(page)` | `GET /trending/movie/week?page={page}` | Home discovery and cold start |
| `recommendedMovies(movieId, page)` | `GET /movie/{movieId}/recommendations?page={page}` | Personalized Home candidates from quiz favourites and Likes |
| `moviesInGenre(genreId, page)` | `GET /discover/movie?with_genres={genreId}&sort_by=popularity.desc&page={page}` | Selected Genre browsing; no rating, vote-count or date filters |
| `movieDetails(movieId)` | `GET /movie/{movieId}?append_to_response=credits` | Movie Details and resolving missing quiz favourite titles |
| `discoverMovies(options, page)` | `GET /discover/movie` with `sort_by`, `page` and optional filters below | Quiz browsing and Home's preferred-genre sources |

`search/movie`, `trending/movie/week`, `recommendations` and `discover/movie` all answer
with the same shape and are parsed into `MoviePageDto`.

All repository list methods default to page 1. The Retrofit methods require their arguments,
except configurable `discoverMovies`, whose defaults are page 1, `popularity.desc` and null
filters. Required application inputs are a nonblank search title, a movie ID for details
or recommendations, and a genre ID for `moviesInGenre`. Retrofit encodes query values.

The repository rejects blank normalized searches and non-positive IDs/pages locally.
For search, trending, recommendations and plain genre browsing, upper page/ID bounds
are enforced by the Worker: page 1–500, movie/genre IDs 1–999999999 without leading zeros,
and search text at most 100 characters. These repository methods do not enforce those
upper bounds themselves.

Configurable discovery maps `MovieDiscovery` to these query parameters:

| Query parameter | Default / mapping | Validation in the repository and Worker |
| --- | --- | --- |
| `with_genres` | `genreId`; null omits it (all genres) | One ID, 1–999999999; no comma/pipe lists |
| `sort_by` | `POPULARITY` → `popularity.desc`; `MOST_VOTED` → `vote_count.desc`; `HIGHEST_RATED` → `vote_average.desc` | Only these three values |
| `vote_average.gte` | `minimumRating`; null omits it | Integer 0–10 |
| `vote_count.gte` | `minimumVotes`; null omits it | Integer 0–2147483647 |
| `primary_release_date.lte` | `releasedBy`; null omits it | `LocalDate` serialized as `YYYY-MM-DD`, year 1000–9999; Worker also checks calendar validity |
| `page` | 1 | 1–500 |

Unlike the other list methods, `discoverMovies` validates all these bounds before calling
the service. The Worker checks supplied parameters, rejects duplicates and unknown names
(including client-supplied `api_key`), but does not require parameters to be present or
supply defaults. The app supplies `page`, discovery sorting, and `append_to_response=credits`
on its requests. It sends no `language`, `region` or `include_adult` override.

Movie Details asks for credits in the same request with `append_to_response=credits`, so one
screen costs one request. The cast and crew arrive in `MovieDetailsDto.credits`.

The ten genres on the Genres screen are fixed in the app (see section 9 of
[design-specs.md](design-specs.md)), so there is no request for the genre list.

The quiz uses `MovieDiscovery.familiar`: `vote_count.desc`, minimum rating 6,
at least 100 votes, and a primary release date no later than the current UTC date.
The same date is retained while paging through one quiz session. With no chosen
genres, the quiz uses these filters without `with_genres`; it does not use trending.
Selected genres are requested separately and interleaved. Title search keeps its
relevance order and allows lower-rated favourites and unknown release dates;
explicitly future-dated movies are excluded.

`Movie` carries optional genre IDs, rating, vote count and a parsed release date
from list responses, making Home ranking possible without per-card details
requests. Sparse saved-list rows may not have this metadata. Discovery cache keys
include every filter, sort order, cutoff date and page, and never reuse the plain
genre or trending cache for a filtered request.

The proxy rules introduced for the quiz must be deployed before distributing that
app version; older Workers reject the discovery parameters with HTTP 400.
Personalized Home (#40) reuses those same endpoints, filters and cache keys, so it
does not require an additional proxy change or deployment. Home requests popular
movies per preferred genre, using the user's minimum rating, at least 100 votes and
a current UTC release cutoff. Favourite/Like recommendations and weekly trending
are merged and filtered locally with the same minimum. See the ranking, request
limits and offline behaviour in [architecture.md](architecture.md#personalized-home-recommendations).

Images are not fetched through this service. Posters are `https://image.tmdb.org/t/p/{size}{path}`
and are loaded separately through Coil from the paths in the responses.

## Example response and mapping

Illustrative response for `GET /search/movie?query=the%20matrix&page=1`, reduced to
one result; values and totals are examples, not a captured live response:

```json
{
  "page": 1,
  "results": [
    {
      "id": 603,
      "title": "The Matrix",
      "poster_path": "/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg",
      "release_date": "1999-03-30",
      "genre_ids": [28, 878],
      "vote_average": 8.2,
      "vote_count": 25000
    }
  ],
  "total_pages": 1,
  "total_results": 1
}
```

The existing definitions in [MovieDtos.kt](../frontend/app/src/main/kotlin/ee/movara/app/data/remote/MovieDtos.kt):

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoviePageDto(
  val page: Int = 1,
  val results: List<MovieSummaryDto> = emptyList(),
  @SerialName("total_pages") val totalPages: Int = 0,
  @SerialName("total_results") val totalResults: Int = 0
)

@Serializable
data class MovieSummaryDto(
  val id: Int? = null,
  val title: String? = null,
  @SerialName("poster_path") val posterPath: String? = null,
  @SerialName("release_date") val releaseDate: String? = null,
  @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
  @SerialName("vote_average") val voteAverage: Double? = null,
  @SerialName("vote_count") val voteCount: Int? = null
)
```

Retrofit's kotlinx.serialization converter uses `tmdbJson` to decode the body into
`MoviePageDto`, including a `MovieSummaryDto` for each result. `@Serializable` enables
generated serializers; `@SerialName` maps snake_case JSON keys such as `poster_path`
and `total_pages` to Kotlin properties `posterPath` and `totalPages`.

Every property has a default. Missing nullable values remain null; missing lists become
empty. With `coerceInputValues = true`, explicit nulls for non-null properties with
defaults also use those defaults. `ignoreUnknownKeys = true` ignores fields the app
does not model. Neither setting fixes an incompatible field type: for example,
`"id": "invalid"` fails deserialization for the response.

`DefaultMovieRepository` calls `MoviePageDto.toDomainMovies()`, which uses
`MovieSummaryDto.toDomainOrNull()` in `TmdbMappers.kt`. It drops entries without a
positive ID or nonblank title, trims text, derives the release year and parsed date,
deduplicates positive genre IDs, and turns unusable ratings into null. The example
becomes a `Movie` with ID 603, title The Matrix, release year 1999, genres 28/878 and
rating 8.2. The repository wraps the movies and pagination in `MoviePage`; UI code
receives domain models rather than DTOs.

## Rate limits and request constraints

[TMDB's official rate-limit guide](https://developer.themoviedb.org/docs/rate-limiting)
says the legacy fixed limit was disabled in 2019. It still applies upper limits to
discourage bulk scraping, but these can change at any time; there is no fixed quota
Movara can rely on. TMDB asks clients to respect HTTP `429`.

The Worker forwards `429` status and body. It forwards only `Content-Type`, so upstream
rate-limit headers such as `Retry-After` are not preserved. The repository tries matching
cached data on `429`; without usable cache it returns an HTTP error with status 429.
Neither the Worker nor the Android repository implements a rate limiter, timed
throttle, retry loop or backoff, and neither schedules a retry from `Retry-After`.
The repository invokes the service once per network attempt. OkHttp retains its
[default connection recovery](https://github.com/square/okhttp/blob/master/okhttp/src/commonJvmAndroid/kotlin/okhttp3/OkHttpClient.kt),
so this is not a guarantee of exactly one wire request.

Application request budgets are separate from TMDB's limits. A cold Home load needs
at most 21 source pages (ten Likes, five quiz favourites, five genres and one trending
page), with at most three source requests concurrent and duplicate seeds requested
once. Fresh cache reuse reduces network calls. Search and quiz title changes use
a 400 ms debounce and cancel superseded work; quiz genre browsing can request up to
five genre pages concurrently. These are feature-level constraints, not a global
requests-per-second limit. See [Home orchestration](architecture.md#personalized-home-recommendations)
for seed selection and ranking.

## Results and failures

Every function suspends and returns a parsed response. A request that fails throws:
`retrofit2.HttpException` for a non-2xx answer, `java.io.IOException` when the phone is
offline or the proxy does not answer, and `kotlinx.serialization.SerializationException`
for a response that cannot be parsed.

| Status | Comes from | Meaning and repository behavior |
| --- | --- | --- |
| `400` | Proxy or TMDB | Invalid/duplicate/unsupported query parameter, or upstream bad request; HTTP error, no cache fallback |
| `401` / `403` | TMDB | Invalid/revoked key or access denied; HTTP error, no cache fallback |
| `404` | TMDB or proxy | Unknown movie or unsupported/malformed path (including an invalid movie ID); HTTP error, no cache fallback |
| `405` | Proxy | Method other than GET; includes `Allow: GET`; HTTP error, no cache fallback |
| `408` / `429` | TMDB, forwarded by proxy | Request timeout or rate limit; try cache, otherwise HTTP error |
| `500` | Proxy or TMDB | Worker missing `TMDB_API_KEY`, or upstream internal error; try cache, otherwise HTTP error |
| `502` | Proxy or TMDB | Worker upstream fetch failed, or upstream bad gateway; try cache, otherwise HTTP error |
| Other `5xx` | TMDB, forwarded by proxy | Upstream service failure; try cache, otherwise HTTP error |

TMDB's own error statuses and bodies pass through the proxy; only `Content-Type` is
forwarded from its headers. Worker-generated errors contain `{"status_message": "..."}`.
The repository uses the status and a generic safe message; it does not display either
provider's raw error body. Worker configuration is checked first, so a missing secret
returns 500 even for a request that would otherwise be rejected.

Callers above the data layer use `MovieRepository` and never see those exceptions.
`RepositoryResult.Error` contains a safe message, category (invalid input, network,
HTTP, invalid response, database or unexpected failure) and optional HTTP status.
Coroutine cancellation is rethrown. Blank search queries and non-positive IDs/pages
return an input error without calling TMDB.

Successful results contain domain models, pagination metadata for lists, a source
(`NETWORK` / `CACHE` / `LOCAL`) and, for TMDB data, the last successful fetch timestamp.
Requests use `NETWORK_FIRST` by default: call the service, then try cache only on a
recoverable failure. Home calls the policy
overloads of `recommendedMovies`, `discoverMovies` and `trendingMovies` with
`PREFER_FRESH_CACHE`: a validated response younger than six hours is returned without
a network request, with `source = CACHE` and `freshCacheHit = true`. Its original fetch
timestamp is preserved. Explicit Home retry uses `NETWORK_FIRST`; later automatic
loads prefer fresh cache again. Search, details, genre browsing and the quiz retain
their existing network-first behaviour.

Validated successful DTOs are serialized into Room's
`tmdb_cache` table (`cacheKey` primary key, `responseJson`, `fetchedAt` in epoch milliseconds)
before returning fresh data. This is a DTO response cache, not an OkHttp HTTP cache; no
metadata HTTP cache is configured. A storage write failure returns usable
network data with `cachePersisted = false`.

Offline/timeouts, HTTP 408/429/5xx, serialization failures and unusable responses try
the matching persistent cache entry. Valid cached data returns `Success(source = CACHE)`;
its timestamp remains unchanged, so the UI can identify stale content. Missing or corrupt
cache data preserves the original remote error. If the fallback database read itself
fails, the result is `Error(category = DATABASE)` instead. Other HTTP failures, including
400/401/403/404/405, remain errors even when cached data exists. Unexpected exceptions
also remain errors without fallback. Failed requests do not overwrite successful entries.
There is no repository retry loop or cache eviction. Cache entries are retained beyond
the six-hour freshness window for offline fallback. Error-fallback responses have `freshCacheHit = false` and can show the
saved/stale-content notice on Home; normal fresh-cache hits do not. Corrupt, unreadable
or future-dated entries are ignored by the fresh-cache check and a network attempt follows.
Fallback validates the cached DTO and domain mapping but applies no freshness or timestamp
check, so even old entries remain eligible.

Cache keys distinguish endpoint, query/IDs and page, for example `search:interstellar:1`,
`trending:1`, `recommendations:603:1`, `genre:28:2` and `details:603`.
Search queries are trimmed, whitespace collapsed and lowercased with Locale.ROOT for
both the key and request. Cached responses survive app restarts, independently of
the user's saved lists. Coil caches image data separately in memory and in a 100 MiB
disk cache under `cacheDir/movie_images`, configured in `MovaraApplication`. Room metadata
availability does not guarantee that artwork is cached; missing or failed images show a
placeholder.

Responses with missing optional fields use the existing `TmdbMappers.kt` behavior.
Unusable cards are dropped; a wholly unusable nonempty page, invalid pagination,
or details without the requested ID and a usable title is treated as an invalid response.
An empty valid page is a successful response and can be cached. Because DTO fields have
defaults, some incomplete JSON (including `{}` for page 1) can map to an empty success;
not every omitted field is an error.

### How failures reach screens

`IOException` (including connection failures and socket timeouts) becomes `NETWORK`;
deserialization or mapping validation failures become `INVALID_RESPONSE` when cache
cannot recover them. Local saved-list reads/writes become `DATABASE` errors. An initial
fresh-cache read failure is ignored so the network can still succeed, and a cache write
failure keeps the fresh result usable. Cancellation propagates rather than showing an error.

ViewModels convert `RepositoryResult` into their own loading, content, empty and error
state. They do not expose transport exceptions or raw server messages:

| Screen/feature | Implemented behavior |
| --- | --- |
| Search / Selected Genre | Loading feedback, empty results, or an error with Retry. A later-page failure preserves existing cards and retries the failed page. |
| Quiz movie picker | Loading/error feedback and Retry; successful genre results can remain visible when another source fails. Draft selections survive failures. |
| Movie Details | Loading placeholders; request errors show Try again and Back. A cached success renders normally; the ViewModel does not expose cache provenance. |
| Home | Keeps successful source batches when others fail, with error/retry feedback. When inputs are unchanged, Retry fetches only failed sources; explicit retries use network-first. Error-fallback cache shows a saved-content notice; fresh-cache reuse does not. |
| Saved lists / membership actions | Observation errors offer storage retry. Write failures preserve the previous snapshot and show a Snackbar with Retry; list actions use the shared store. |

Search, Selected Genre and the quiz also consume cached successes as normal content
without a per-response stale notice. The navigation-level offline banner is driven
separately by connectivity monitoring; it does not prove TMDB is reachable, suppress
repository requests, or automatically retry them when connectivity returns.

## Logging

With `BuildConfig.ENABLE_HTTP_LOGGING` the client logs request and response bodies. The flag
is `true` for debug and staging and `false` for release; release installs no HTTP logging
interceptor.

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
