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
By default the repository makes one network attempt and persists validated DTO responses
before returning fresh data. Home opts into validated cache reuse for up to six hours
through `MovieRequestPolicy.PREFER_FRESH_CACHE`; these hits make no network attempt. A cache write failure keeps the fresh result usable and reports
`cachePersisted = false`. Recoverable failures load the matching Room cache entry;
cached results may be stale. Cancellation propagates normally.

Saved-list observations, counts and membership are flows of results containing domain
data. Save/remove operations delegate to the existing DAO. List business rules (#39),
connectivity UI (#43) and image caching (#48) remain separate features.
Home recommendation orchestration and ranking are described below.

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
entries; fallback reads and validates the persisted response. Entries are retained for
offline fallback even after Home's six-hour freshness window expires. There is no eviction
or automatic retry. This response cache never changes a user's saved-list memberships.

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
or `SKIPPED`. Favourite titles are saved as optional display metadata in the same
atomic update, so reopening the quiz does not require network access to recognize
saved selections. Quiz favourites are separate from the Room Like, Watch Later and
Watched lists; selecting one does not change any list membership.

Older profiles containing only IDs remain readable. Missing titles can be resolved
through movie details, but every selected ID always has a removable chip, using a
numbered fallback label when its title is unavailable. Unreadable optional title
metadata is ignored without discarding IDs or resetting onboarding status.

Draft answers belong to the quiz UI state. Saving atomically replaces all preferences
and marks onboarding completed; later edits use the same operation. Skipping marks
a new profile as skipped without overwriting a completed profile. Both operations
survive app restarts. The profile Flow exposes persisted data only. Read and write
failures propagate to the caller instead of silently resetting the user's preferences.
Database and DataStore files are excluded from backup and device transfer.

Before constructing the normal navigation graph, `PreferenceStartupViewModel` reads
the stored profile. New installations see the three-step genre, favourite-film and
minimum-rating quiz. Completed/skipped profiles go directly to Home. A storage-read
failure offers retry or entry to Home for the current session without changing the
stored status.

`MoviePreferenceQuizViewModel` keeps draft answers, the current step and search query
in `SavedStateHandle`; leaving an edit without saving does not change the profile.
The movie picker uses shared `MovieDiscovery.familiar` options: most-voted first,
minimum rating 6, at least 100 votes, and already released by the current UTC date.
It interleaves selected genres' paginated results, or discovers across all genres
when none are selected. There are no decade quotas or weighted scores. It also
supports debounced title search without the browsing quality floor; search accepts
unknown release dates but excludes explicitly future-dated films. Selections persist
across searches and pages. **Show more** remains available when filtering removes
all displayed results but further pages exist. Home's **Your taste** action opens the same quiz
with existing preferences. Saving errors retain the draft for retry; first-launch
users can also continue for the current session without saving.

## Personalized Home recommendations

Home observes the persisted taste profile and the complete Like/Watched lists.
`HomeRecommendationLoader` requests the first recommendation page for up to ten
Likes and five quiz favourites (stable ID order); overlapping seed IDs are requested
only once. `RecommendationSeeds.selectLikes` always includes the five most recent Likes.
For at most ten Likes, every Like is used. Larger histories also contribute five older
Likes from ID-sorted, consecutive windows that rotate with the UTC date. The selection
is stable for an unchanged history and date, covers the older history over successive
days, and is recalculated on the next Home load; there is no midnight background job. It also requests popular movies for each preferred genre,
using `MovieDiscovery.familiar` with popularity sorting and the user's minimum rating,
plus one weekly-trending page for discovery. At most 21 source pages are needed for
a cold load: ten Likes, five quiz favourites, five genres and one trending page.
`MovieTastePreferences` enforces `MAX_GENRES = 5` on construction, including when
DataStore reconstructs a saved profile. A persisted profile with more genres produces
a read error before Home can load its sources; it is not silently truncated.
The existing preference-validation and Home request-budget tests cover these limits.
At most three requests run concurrently.
All selected favourites and all Likes remain excluded, including those beyond the
seed request limit. Missing favourite titles do not require extra detail requests.

`MovieRecommendations.rank` is a pure function. It merges candidates by TMDB ID and
records distinct seed/genre/discovery sources, so duplicates do not inflate overlap.
It excludes Watched, all favourite/liked seed films and explicitly future-dated films.
An explicit minimum rating applies to every source, including cached and discovery
results; unknown or lower ratings cannot satisfy it. The minimum is never lowered.

Eligible films are ordered by these rules, in order:

1. Number of distinct favourite/liked movie recommendation sources.
2. Number of preferred genres matched by metadata or a genre-discovery source.
3. At least 100 community votes before less-established ratings.
4. TMDB rating, then vote count, descending; finally TMDB ID ascending for stable ties.

There are no weighted scores or decade quotas. The highest-ranked eligible result
is Top Pick. At most 24 films are retained, with at most four discovery-only films
when personalized candidates exist. After Top Pick, presentation prefers the next
ranked candidate with a different source and genre from the preceding film, when
available, otherwise retaining ranking order. With no preferences or Likes, discovery
can fill all 24 places. Reasons identify an enjoyed film, a preferred genre, or trending.

The spotlight and remaining movie row share this deduplicated selection; Top Pick
is not repeated in the row. Profile or Like changes cancel stale loads and refresh
sources. Watched changes immediately refilter/rerank fetched candidates without
network requests; removing a Watched entry can restore a candidate. Watch Later
changes update action membership without changing recommendations.

Home reuses validated persistent MovieRepository responses younger than six hours,
without extending their fetch timestamp. New Likes fetch only source pages that are
missing or no longer fresh; unchanged sources reuse their cached pages. Genre/rating/date
filters remain part of each cache key. Corrupt, unreadable or future-dated cache entries
do not prevent a network attempt. A `freshCacheHit` is normal reuse and does not show
the stale/offline notice. Error-fallback cached results remain labelled.
Individual source failures keep successful results visible with retry feedback.
A missing profile or unreadable Like/Watched list cannot silently bypass saved filters.
Retry resubscribes to profile and Watched observations and makes network-first
source requests, bypassing fresh-cache reuse while retaining offline fallback.
Subsequent automatic Like/profile changes prefer fresh cache again. Empty
results explain an active minimum rating and leave **Your taste** accessible.
Discovery cache keys still include the exact cutoff date: an offline session on a
new date may lack genre-cache entries, while previously cached seed/trending results
can remain available. No new endpoints or proxy deployment are required for #40.

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
