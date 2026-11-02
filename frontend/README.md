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

Android Studio stores your SDK path in the ignored `local.properties` file. The next section adds your TMDB API key to the same file. No backend or user account is required.

## 3. Add your TMDB API key

Movara reads movie data from [TMDB](https://www.themoviedb.org/). Every developer uses their **own** key. The key is never committed, so you have to add it once after cloning.

1. Create a free TMDB account and open **Settings > API**.
2. Request an API key for a personal/educational project and copy the **API Key (v3 auth)** value.
3. Open `local.properties` in the **repository root**. Android Studio creates this file during the first import; if it is missing, create it yourself.
4. Add your key on its own line, without quotes:

   ```properties
   TMDB_API_KEY=your_key_here
   ```

5. In Android Studio, click **File > Sync Project with Gradle Files**. The build picks up the new value on the next sync or build; you do not have to restart Android Studio.

The build reads this value and exposes it to the app as `BuildConfig.TMDB_API_KEY`. If the key is missing, the build still succeeds but prints a warning and TMDB requests fail, so check the **Build** output first when the app shows no movies.

Rules for the key:

- `local.properties` is listed in the repository's `.gitignore`. **Do not remove that line and do not force-add the file.** The same applies to `*.jks` and `*.keystore` signing files.
- Never paste a key into a source file, a test, a commit message, a pull request, or a screenshot.
- If a key does end up in a commit, treat it as leaked: revoke it in TMDB's settings and create a new one. Deleting the line in a later commit does not remove it from the Git history.
- A build server has no `local.properties`. It supplies the key through a `TMDB_API_KEY` environment variable, which the build uses as a fallback.

`BuildConfig.TMDB_API_KEY` is compiled into the APK, so it is readable by anyone who unpacks the build. This step keeps the key out of **source control**; it does not make the key secret from a user of the app. That is the expected trade-off for a TMDB client key.

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

## 6. Enable automatic Compose UI updates

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
- **No movies load / `TMDB_API_KEY is not set` in the build output:** add your key to the repository root's `local.properties` as described in section 3, then sync Gradle again.
