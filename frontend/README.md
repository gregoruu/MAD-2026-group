# Movara Android

## 1. Open the project in Android Studio

1. Install Android Studio **Panda 3 (2025.3.3 Patch 1) or newer**.
2. From the welcome screen, click **Open**. If another project is open, use **File > Open**.
3. In the folder where you cloned this repository, select **the root `settings.gradle.kts` file**.
4. Choose **Open as Project** if prompted, and trust the project.
5. Wait for the Gradle import and downloads to finish. The first import requires internet access.

Open the repository root to keep **backend**, **frontend**, and **docs** in the same window. There is one Gradle project, configured at the repository root. Its Android application module is `frontend/app`.

Build configuration is organized as follows:

- Root `settings.gradle.kts`: includes the Android module at `frontend/app`.
- Root `build.gradle.kts` and `gradle.properties`: shared build configuration.
- Root `gradle/libs.versions.toml`: dependency and plugin versions.
- Root `gradle/wrapper/` and `gradlew` / `gradlew.bat`: Gradle Wrapper, also used by Android Studio.
- `frontend/app/build.gradle.kts`: app module configuration (SDK levels, dependencies, and build types).
- `.run/Movara.run.xml`: the shared Android Studio run configuration.

`frontend/` is not a separate Gradle project. Keep the configuration files above; the app module build file is part of the root project. Folders named `.gradle/` (with a leading dot) are generated caches, not another copy of the build configuration.

If you previously opened the repository as a plain folder or linked frontend separately, first choose **File > Close Project**, then open the root settings file as described above.

In the left file pane, select **Project** from the dropdown above the file tree to see the whole repository. The **Android** view only shows Android-related content.

## 2. Install the Android SDK (first time)

1. Open **Tools > SDK Manager**.
2. Under **SDK Platforms**, install **Android SDK Platform 37.0**.
3. Under **SDK Tools**, install **Android SDK Build-Tools 36.0.0**, **Android SDK Platform-Tools**, and **Android Emulator**.
4. Click **Apply**, accept the licenses, and wait for installation to finish.
5. Under **Settings > Build, Execution, Deployment > Build Tools > Gradle**, use Android Studio's bundled JDK as the **Gradle JDK**.

Android Studio stores your SDK path in the ignored `local.properties` file. No user account or TMDB API key is required.

## 3. How the app reaches TMDB

Movara reads movie data from [TMDB](https://www.themoviedb.org/). The app does not talk to TMDB directly: it sends its requests to the TMDB proxy in `backend/`, a Cloudflare Worker that adds the API key and forwards them. The key is stored only in Cloudflare, so no build of the app contains it, and you do not need a key of your own.

| Build type | Proxy |
| --- | --- |
| `debug`, `staging` | `movara-tmdb-proxy-staging` |
| `release` | `movara-tmdb-proxy-production` |

The build exposes the proxy address to the app as `BuildConfig.TMDB_BASE_URL`. Running, deploying and configuring the proxy is described in `backend/README.md`.

Movie images come from `image.tmdb.org`, which needs no key, so they do not go through the proxy.

You can check that the staging proxy and TMDB answer, without starting the app:

```
./gradlew :app:testDebugUnitTest -PrunLiveTmdbTests --tests "*TmdbApiServiceLiveTest"
```

These tests call the real API and need internet access. They only run when you ask for them
with `-PrunLiveTmdbTests` (a build server can use a `RUN_LIVE_TMDB_TESTS` environment
variable instead); otherwise they are skipped. A plain `./gradlew test` therefore never
depends on the network or on the proxy being up. The rest of the test suite never touches
the network either.

Rules for keys and secrets:

- `local.properties` is listed in the repository's `.gitignore`. **Do not remove that line and do not force-add the file.** The same applies to `*.jks` and `*.keystore` signing files.
- Never paste a key into a source file, a test, a commit message, a pull request, or a screenshot.

## 4. Create and start an emulator

Create a virtual phone once:

1. Open **Tools > Device Manager**.
2. Click **+ / Create Virtual Device**.
3. Choose a phone such as **Pixel 6** and click **Next**.
4. Select a stable **API 37** system image and download it if necessary. Use the architecture recommended for your computer.
5. Click **Next > Finish**.

Start the virtual phone:

1. In **Device Manager**, click **▶** beside your virtual phone.
2. Wait until the phone finishes booting and displays its home screen.
3. Leave the emulator running.

On later runs, reuse the existing virtual phone.

## 5. Build, install, and open Movara

1. In Android Studio's **main top toolbar**, find the **run configuration dropdown**. Select **Movara**, the shared Android App configuration included in this repository.
2. In the **device selector** in the same toolbar, select your running Pixel emulator. The emulator tab in the Running Devices panel is a display, not the run configuration selector.
3. Click the main toolbar's green **▶ Run** button, or choose **Run > Run 'Movara'**.
4. Wait for the build and installation. Movara opens on the emulator and displays **Welcome to Movara**.

For everyday use: **open the project → start the emulator → select Movara and the emulator → click Run**.

If the configuration is missing or its module list is empty, the Android project has not imported successfully. Check the Gradle import error in the Build output before creating another run configuration.

## 6. Build variants

The app module has three build types. Everyday development uses **debug**. **staging** is the build the team installs and tests; **release** is what a shipped build would be.

| Build type | Application id | App name | Debuggable | HTTP logging | Shrinking | Signed with |
|---|---|---|---|---|---|---|
| `debug` | `ee.movara.app` | Movara | yes | on | off | debug keystore |
| `staging` | `ee.movara.app.staging` | Movara (Staging) | yes | on | **on** | debug keystore |
| `release` | `ee.movara.app` | Movara | no | off | on | release keystore |

`staging` has its own application id, so it installs next to a release build on the same device instead of replacing it. Its version name ends in `-staging`.

**Staging is shrunk on purpose.** It is the build the team installs, so it has to be the one that proves R8 works. If only `release` were shrunk, nothing anybody runs would exercise the shrinking rules and a missing keep rule would stay hidden until it was too late to fix. The price is that stack traces from a staging build are obfuscated; the mapping file that decodes them is written to `frontend/app/build/outputs/mapping/staging/`. Use a **debug** build when you need a readable stack trace or the Layout Inspector.

Build them from the command line:

```
./gradlew :app:assembleDebug
./gradlew :app:assembleStaging
./gradlew :app:assembleRelease
```

The packages appear under `frontend/app/build/outputs/apk/<build type>/`. In Android Studio, switch between build types in **Build > Select Build Variant**.

Code can branch on the current variant through `BuildConfig.ENABLE_HTTP_LOGGING`, which is `true` for debug and staging and `false` for release. Use that flag instead of checking `BuildConfig.DEBUG` when deciding whether to attach the OkHttp logging interceptor, so staging logs too.

### Signing a release build

The release keystore is **never committed**. `*.jks` and `*.keystore` are listed in `.gitignore`. Keep the file outside the repository and point at it from `local.properties`:

```properties
RELEASE_KEYSTORE_PATH=C:/path/to/movara-release.jks
RELEASE_KEYSTORE_PASSWORD=...
RELEASE_KEY_ALIAS=movara
RELEASE_KEY_PASSWORD=...
```

Use forward slashes even on Windows. `local.properties` is read with `java.util.Properties`, which treats `\` as an escape character. A relative path is resolved against the repository root. A build server can supply the same four values as environment variables instead.

If **none** of the four values are set, the release variant still builds but comes out as `app-release-unsigned.apk` and cannot be installed on a device. The build prints a warning saying so. That is the normal case for everyone who is not preparing a shared build.

If **some** are set and some are not, or the path does not point at a file, the release build **fails** with a message naming the problem. A half-configured keystore means somebody wanted a signed build, so the build stops rather than handing back an unsigned package that looks finished.

Only the person preparing a shared build needs the keystore. Everyone else can ignore this section; debug and staging are signed with the local debug keystore that Android Studio manages for you.

## 7. Enable automatic Compose UI updates

1. Open **File > Settings > Editor > Live Edit**. On macOS, Settings is under the **Android Studio** menu.
2. Enable **Live Edit** and choose **Automatic**.
3. Click **Run 'Movara'** to deploy the app through Android Studio.
4. Edit an existing Compose function body in Android Studio. Supported UI changes appear automatically in the emulator.

Live Edit requires API 30 or newer; the API 37 emulator meets this requirement. Adding functions, changing function signatures, or changing build configuration requires clicking **Run** again.

## Troubleshooting

- **Backend is missing from the sidebar:** switch its dropdown from **Android** to **Project** and expand the repository root.
- **No Movara configuration / no modules / Cannot obtain the package:** close the project and reopen the repository root's `settings.gradle.kts` as an Android Gradle project. Wait for import to finish and inspect any import errors. Open the root project; `frontend/` only contains the app module and is not a separate project.
- **No target device:** start your virtual phone in Device Manager and wait for its home screen.
- **Emulator fails to boot:** use the virtual device's menu in Device Manager and choose **Cold Boot**.
- **Build fails:** inspect the first relevant error in the **Build** tool window.
- **No movies load:** run the live tests from section 3. A `500` with `The proxy is not configured.` means the proxy has no `TMDB_API_KEY` secret; a `401` means TMDB rejected the key. Both are fixed in Cloudflare, not in the app; see `backend/README.md`.
- **`The release build is not signed` / you only get `app-release-unsigned.apk`:** none of the four `RELEASE_*` values are set. See section 6. Debug and staging builds do not need them, so this is expected unless you are preparing a shared build.
- **`Release signing is incomplete` or `no keystore was found at …`:** some of the four values are set and some are not, or the path does not point at a file. The message names the resolved path and which values are blank. Note that a Windows path must use forward slashes — `local.properties` is read with `java.util.Properties`, which treats `\` as an escape character, so `C:\keys\movara.jks` silently becomes `C:keysmovara.jks`.
- **Release build crashes where debug works:** only the release build shrinks code, so the cause is usually a missing R8 keep rule. Add it to `frontend/app/proguard-rules.pro` and say in a comment which library needs it.

## Image loading

Movie images use the shared TmdbImage component and an application-wide
Coil ImageLoader with crossfade enabled.

Cards and rows use w342, Spotlight uses w500, and Movie Details
backgrounds should use w780.

The Movie Details screen is not implemented yet; its w780 integration still
needs to be completed under issue #36. Genre tiles share the same loading
and error rendering while keeping their image URLs unchanged.

Missing or failed images show a dark placeholder with a movie icon.
The disk cache stores up to 100 MiB of images. Previously loaded images
can appear offline while they remain cached. Movie metadata is not
stored by this image cache.

### Automated image tests

Run the URL model tests and the existing unit suite with
`./gradlew :app:testDebugUnitTest :app:lintDebug`.
`TmdbImageModelTest` covers null/blank paths, trimming, relative paths,
absolute URLs, and the w342/w500/w780 sizes.

With an Android device or emulator connected, run:

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ee.movara.app.ui.components.MovieImageTest,ee.movara.app.ui.components.MovieImageCacheTest
```

The five UI tests cover missing paths without starting requests, malformed
URLs, loading failures, model changes between error/loading/success, and an
old request completing after the model changes. Controlled Coil interceptors
hold requests in loading until each test releases them.

The cache test uses the application's 100 MiB disk cache, real PNG decoding,
and a controlled OkHttp transport. Memory caching is disabled. It verifies
that the second request succeeds from disk without invoking the transport,
which fails any subsequent network call. These tests need no TMDB key or
external image server.

