# Movara Android

## 1. Open the project in Android Studio

1. Install Android Studio **Panda 3 (2025.3.3 Patch 1) or newer**.
2. From the welcome screen, click **Open**. If another project is open, use **File > Open**.
3. Select **settings.gradle.kts in the repository root**, for example:
   `C:\Users\sande\MAD-2026-gruop\settings.gradle.kts`
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

Android Studio stores your SDK path in the ignored `local.properties` file. No backend, API keys, or credentials are required for the current starter app.

## 3. Create and start an emulator

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

## 4. Build, install, and open Movara

1. In Android Studio's **main top toolbar**, find the **run configuration dropdown**. Select **Movara**, the shared Android App configuration included in this repository.
2. In the **device selector** in the same toolbar, select your running Pixel emulator. The emulator tab in the Running Devices panel is a display, not the run configuration selector.
3. Click the main toolbar's green **▶ Run** button, or choose **Run > Run 'Movara'**.
4. Wait for the build and installation. Movara opens on the emulator and displays **Welcome to Movara**.

For everyday use: **open the project → start the emulator → select Movara and the emulator → click Run**.

If the configuration is missing or its module list is empty, the Android project has not imported successfully. Check the Gradle import error in the Build output before creating another run configuration.

## 5. Enable automatic Compose UI updates

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
