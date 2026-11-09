# Android architecture

The Android module is `frontend/app`; production packages live under
`frontend/app/src/main/kotlin/ee/movara/app`.

| Package | Code that belongs here |
| --- | --- |
| `ee.movara.app` (root package) | App entry points (`MainActivity`, `MovaraApplication`), dependency wiring (`AppContainer` and its extensions), and app-level navigation graphs and keys (`Navigation`, `NavigationKeys`) |
| `data/remote` | Retrofit services, network DTOs and remote data sources |
| `data/local` | Room database, DAOs, entities and local data sources |
| `data/repository` | Repository interfaces and implementations; source coordination and mapping to domain models |
| `domain/model` | Shared plain Kotlin app models, independent of network, database and UI types |
| `ui/<screen>` | A screen's composables, one ViewModel and its UI state/events |
| `ui/theme` | Shared Compose colours, typography and theme |

Use lowercase screen packages such as `ui/home`, `ui/search`, `ui/genres`,
`ui/moviedetails`, `ui/mymovies` and `ui/about` as those features are implemented.
The starter welcome screen stays in `ui/main`. Keep UI state in its screen package;
put reusable movie/genre models in `domain/model`. Package README files retain
the currently unused folders in Git.

Data flow: **Screen → ViewModel → repository → remote/local source**.
State flows back to the screen through a read-only `StateFlow`, collected with
`collectAsStateWithLifecycle`. ViewModels accept repository interfaces through
their constructors and never call Retrofit services or DAOs directly. Repositories
own data access, mapping and cache/persistence decisions. Screens render state and
forward user actions to their ViewModel.

Use simple manual dependency injection for now: `MovaraApplication` owns one
`AppContainer`, which creates shared repository instances lazily. `MainActivity`
passes the container to navigation, where lifecycle-managed ViewModels are created
with `viewModel { ScreenViewModel(container.repository) }`. Do not construct
repositories inside screen composables or ViewModels. Add future services, databases
and repositories to the container; Hilt can replace it if dependency wiring grows.

`MainScreenViewModel` demonstrates the boundary using the starter
`DataRepository`; it is a greeting stub, not the movie backend. Room and the Retrofit
service are configured through AppContainer. Mirror production packages under `src/test/kotlin`
and `src/androidTest/kotlin`; use fake repository interfaces for ViewModel tests
and pass plain state/data to screen UI tests and previews.

## TMDB API

Movie data comes from TMDB over Retrofit and OkHttp. `AppContainer` owns one HTTP client
and one `TmdbApiService`; service functions are `suspend` functions that return parsed
responses and throw on a failed request. The API key is added by an interceptor, not by
the service functions, and HTTP logging follows `BuildConfig.ENABLE_HTTP_LOGGING`.

All movie ViewModels share the `MovieRepository` interface through constructor injection.
`AppContainer.movieRepository` owns one lazy `DefaultMovieRepository`, using the shared
`TmdbApiService`, `TmdbCacheDao` and `SavedMovieDao`. The greeting demo is unchanged.
Repositories map responses to domain models, with `MoviePage` retaining pagination metadata.
ViewModels and screens do not see Retrofit types, Room entities or network exceptions.

`RepositoryResult` represents success or a safe categorized error. Success identifies
network, cache or local-list data; TMDB results include the last successful fetch time.
The repository makes one network attempt and persists validated DTO responses before
returning fresh data. A cache write failure keeps the fresh result usable and reports
`cachePersisted = false`. Recoverable failures load the matching Room cache entry;
cached results may be stale. Cancellation propagates normally.

Saved-list observations, counts and membership are flows of results containing domain
data. Save/remove operations delegate to the existing DAO. List business rules (#39),
Home recommendation logic (#40), connectivity UI (#43) and image caching (#48) remain
separate features.

The endpoints, the failure behaviour and the logging rules are in
[docs/api-docs.md](api-docs.md).

## Local database

Movara stores the user's movie lists in the Room database `movara.db`.
The current database version is 2. The explicit `MIGRATION_1_2`, registered in
AppContainer, adds `tmdb_cache` without changing existing saved movies or their index.
There is no destructive fallback.

The `saved_movie` table uses `(tmdbId, listType)` as its composite primary
key, allowing a movie to belong to multiple lists independently.
DAO queries expose Flow values, ordered by newest addition first,
and provide a movie count for each list.

The separate `tmdb_cache` table stores a request key, serialized TMDB DTO and successful
fetch timestamp. Keys distinguish endpoints, normalized queries, movie/genre IDs and
individual pages, including movie details. Failed requests do not replace successful
entries; fallback reads and validates the persisted response. There is no expiry or
automatic retry. This response cache never changes a user's saved-list memberships.

Database schemas are exported to `frontend/app/schemas` and committed
to version control.

Future schema changes must increment the database version and provide
a tested migration that preserves existing user lists. Destructive
migration fallback is not used because the lists contain user data.

AppContainer owns the database instance. Repositories access DAOs;
screens and ViewModels do not access the database directly.

## Movie taste preferences

The first-launch quiz and Home recommendations share the `MovieTasteRepository`
interface. `AppContainer` owns its `DataStoreMovieTasteRepository` implementation,
backed by one application-scoped Preferences DataStore in
`files/datastore/movie_taste.preferences_pb`.

The profile stores up to five preferred genre IDs, favourite movie IDs, an optional
minimum TMDB rating (6, 7 or 8), and an onboarding status: `NOT_STARTED`, `COMPLETED`
or `SKIPPED`. Quiz favourites are separate from the Room Like, Watch Later and
Watched lists; selecting one does not change any list membership.

Draft answers belong to the quiz UI state. Saving atomically replaces all preferences
and marks onboarding completed; later edits use the same operation. Skipping marks
a new profile as skipped without overwriting a completed profile. Both operations
survive app restarts. The profile Flow exposes persisted data only. Read and write
failures propagate to the caller instead of silently resetting the user's preferences.
Database and DataStore files are excluded from backup and device transfer.

## Permissions and user accounts

The app declares two Android platform permissions:

| Permission | Purpose |
| --- | --- |
| `INTERNET` | Request movie metadata and poster images from TMDB |
| `ACCESS_NETWORK_STATE` | Allow connectivity checks for offline handling |

Both are normal, install-time permissions. No runtime permission dialog is needed.
The app does not request access to the camera, microphone, location, contacts or
shared storage. Room and the image cache use app-private storage without a storage
permission. See [Android network permissions](https://developer.android.com/develop/connectivity/network-ops/connecting).
Connectivity monitoring and offline UI are planned under issue #43.

### Why there are no user accounts

Stage 1 scoped Movara as a personal, local movie-list app. Liked, Watch Later and
Watched entries belong to this installation and are stored in Room on the device.
There is no registration, login, password storage or account-based synchronization.
The app does not upload these lists or collect account details such as names,
email addresses or passwords.

TMDB supplies public movie data directly to the Android app. Film searches and
movie requests still go to TMDB, which receives the requested data, the app's API
key and normal network metadata such as the source IP address. The TMDB developer
key authorizes API access; it is not a Movara user's login credential.

### Uninstalling, backups and device changes

Uninstalling the app or clearing its storage deletes the local Room database and
its movie lists. Movara has no server copy, export or account recovery feature,
so users should expect a fresh installation to start with empty lists.

The manifest sets `allowBackup="false"` and `fullBackupContent="false"` to disable
Android cloud backup and legacy full backup. For Android 12 and newer,
`res/xml/data_extraction_rules.xml` also excludes the database directory from
cloud backup and device-to-device transfer, including Room's journal files.
These transfer rules are explicit because some manufacturers do not apply
`allowBackup` to device-to-device migration. This is the app's configured policy;
device-specific transfer behavior has not been verified on every manufacturer.
See [Android backup and transfer rules](https://developer.android.com/identity/data/autobackup).

### What adding accounts would require

Accounts would require an authentication provider or backend, secure session
handling, server-side storage tied to a verified user ID, and authorization that
keeps each user's lists private. The repository would need local/remote
synchronization with conflict resolution and a migration path for existing local
lists. Privacy, retention and account/data deletion behavior would also need to be
defined before shipping that feature. These changes are outside the scope of Stage 3.

## Decisions

Choices that were not obvious, and what was rejected along the way, are recorded one
file at a time in [docs/decisions](decisions/README.md). Read the decision before
changing the thing it describes.
