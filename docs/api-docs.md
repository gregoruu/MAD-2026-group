# TMDB API layer

Movara reads all movie data from [TMDB](https://developer.themoviedb.org/docs). There is no
Movara server; the Android app calls TMDB directly. The code lives in
`frontend/app/src/main/kotlin/ee/movara/app/data/remote`.

## The client

| Piece | File | What it is |
| --- | --- | --- |
| Base URL | `TmdbNetwork.kt` | `https://api.themoviedb.org/3/` |
| HTTP client | `TmdbNetwork.kt` | OkHttp, 10 s connect timeout, 20 s read timeout |
| Authentication | `TmdbApiKeyInterceptor.kt` | Adds `api_key` to the query string of every request |
| Endpoints | `TmdbApiService.kt` | Retrofit interface, one `suspend` function per endpoint |
| Parsing | `TmdbJson.kt` | kotlinx.serialization, `ignoreUnknownKeys` and `coerceInputValues` on |
| Wiring | `AppContainer.kt` | One shared client and one service for the whole app |

The app holds a **v3 API key**, read from `local.properties` as `BuildConfig.TMDB_API_KEY`.
See section 3 of `frontend/README.md` for adding it and
[decision 1](decisions/0001-tmdb-api-key-in-the-client.md) for why the key sits in the
client at all. No service function takes the key as a parameter — the interceptor adds it,
so a new endpoint cannot forget it.

Network access requires the `INTERNET` permission, declared in `AndroidManifest.xml`.

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
`retrofit2.HttpException` for a non-2xx answer (401 for a missing or wrong key, 404 for an
unknown movie), `java.io.IOException` when the phone is offline or TMDB does not answer, and
`kotlinx.serialization.SerializationException` for a response that cannot be parsed.

Callers above the data layer never see those exceptions. The repository catches them and
turns them into state the screen can render.

Responses with missing fields do not throw; `TmdbMappers.kt` drops what it cannot use.

## Logging

With `BuildConfig.ENABLE_HTTP_LOGGING` the client logs request and response bodies. The flag
is `true` for debug and staging and `false` for release, so a shipped build logs nothing.

The log redacts the value of `api_key`, so the key is not readable in logcat even on a
staging build handed to someone else.

## Tests

`TmdbApiServiceTest` runs every endpoint against a local MockWebServer and checks the
request that leaves the client, the parsed response, the API key on each request and the
redaction in the log.

`TmdbApiServiceLiveTest` calls the real TMDB API and parses the answer. It is skipped when
no key is configured:

```
./gradlew :app:testDebugUnitTest --tests "*TmdbApiServiceLiveTest"
```
