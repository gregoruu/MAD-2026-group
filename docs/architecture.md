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
`DataRepository`; it is a greeting stub, not the movie backend. Retrofit and Room
will be added with their features. Mirror production packages under `src/test/kotlin`
and `src/androidTest/kotlin`; use fake repository interfaces for ViewModel tests
and pass plain state/data to screen UI tests and previews.

## Decisions

Choices that were not obvious, and what was rejected along the way, are recorded one
file at a time in [docs/decisions](decisions/README.md). Read the decision before
changing the thing it describes.
