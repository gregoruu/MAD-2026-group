# Remote data

Package: `ee.movara.app.data.remote`.

Put Retrofit service interfaces, network DTOs and remote data sources here.
Repositories map DTOs to domain models. Screens and ViewModels must not access this package.

`TmdbApiService` holds every TMDB endpoint the app calls, and `TmdbNetwork.kt` builds the
client and the service. Requests go to the TMDB proxy in `backend/`, which adds the API key.
A new endpoint is one `suspend` function on the service, and its path and query parameters
also have to be allowed in the proxy; see `backend/README.md`.

The endpoints, the error behaviour and the logging rules are documented in
`docs/api-docs.md`.
