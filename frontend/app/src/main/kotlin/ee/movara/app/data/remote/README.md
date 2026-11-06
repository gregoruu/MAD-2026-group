# Remote data

Package: `ee.movara.app.data.remote`.

Put Retrofit service interfaces, network DTOs and remote data sources here.
Repositories map DTOs to domain models. Screens and ViewModels must not access this package.

`TmdbApiService` holds every TMDB endpoint the app calls, `TmdbNetwork.kt` builds the
client and the service, and `TmdbApiKeyInterceptor` adds the API key to every request.
A new endpoint is one `suspend` function on the service; it must not take the key.

The endpoints, the error behaviour and the logging rules are documented in
`docs/api-docs.md`.
