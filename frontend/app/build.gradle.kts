import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

// Secrets are never committed. Developers keep them in the ignored local.properties file
// at the repository root; build servers pass them as environment variables of the same
// name. Both sources are read through the provider API, so the configuration cache is
// invalidated when a value changes.
val localProperties: Properties = Properties().apply {
  val text = providers.fileContents(
    isolated.rootProject.projectDirectory.file("local.properties")
  ).asText.orNull
  if (text != null) {
    load(text.reader())
  }
}

fun readSecret(name: String): String {
  val fromLocalProperties = localProperties.getProperty(name).orEmpty().trim()
  if (fromLocalProperties.isNotEmpty()) {
    return fromLocalProperties
  }
  return providers.environmentVariable(name).orNull.orEmpty().trim()
}

// The TMDB proxy in backend/. Debug and staging builds use its staging Worker, release
// builds use its production Worker.
val workersDevSubdomain = "movara"
val stagingTmdbBaseUrl = "https://movara-tmdb-proxy-staging.$workersDevSubdomain.workers.dev/"
val productionTmdbBaseUrl = "https://movara-tmdb-proxy-production.$workersDevSubdomain.workers.dev/"

// Release signing. The keystore itself is never committed; *.jks and *.keystore are
// ignored by Git. Only its location and passwords come from local.properties.
val releaseKeystorePath: String = readSecret("RELEASE_KEYSTORE_PATH")
val releaseKeystorePassword: String = readSecret("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias: String = readSecret("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String = readSecret("RELEASE_KEY_PASSWORD")

// A relative path is resolved against the repository root, because that is where the
// local.properties file holding it lives.
val releaseKeystore: File? = if (releaseKeystorePath.isEmpty()) {
  null
} else {
  val givenPath = File(releaseKeystorePath)
  if (givenPath.isAbsolute) {
    givenPath
  } else {
    isolated.rootProject.projectDirectory.asFile.resolve(releaseKeystorePath)
  }
}

val releaseSigningValues: Map<String, String> = mapOf(
  "RELEASE_KEYSTORE_PATH" to releaseKeystorePath,
  "RELEASE_KEYSTORE_PASSWORD" to releaseKeystorePassword,
  "RELEASE_KEY_ALIAS" to releaseKeyAlias,
  "RELEASE_KEY_PASSWORD" to releaseKeyPassword
)

val blankReleaseSigningValues: List<String> = releaseSigningValues
  .filterValues { value -> value.isEmpty() }
  .keys
  .toList()

// Nothing set at all is the normal case: most developers never sign a release.
val releaseSigningIsAbsent: Boolean = blankReleaseSigningValues.size == releaseSigningValues.size

// Half-configured signing is a different situation and must not quietly produce an
// unsigned package. Whoever set these values wants a signed build, so the release build
// fails instead, naming what is wrong.
val releaseSigningProblem: String? = when {
  releaseSigningIsAbsent -> null

  blankReleaseSigningValues.isNotEmpty() ->
    "Release signing is incomplete. These values are missing from local.properties " +
      "or the environment: " + blankReleaseSigningValues.joinToString(", ") + "."

  releaseKeystore != null && !releaseKeystore.isFile ->
    "Release signing is configured, but no keystore was found at " +
      releaseKeystore.absolutePath + ". Note that RELEASE_KEYSTORE_PATH is read with " +
      "java.util.Properties, which treats a backslash as an escape character, so write " +
      "the path with forward slashes."

  else -> null
}

val releaseSigningIsConfigured: Boolean = !releaseSigningIsAbsent && releaseSigningProblem == null

android {
  namespace = "ee.movara.app"
  compileSdk {
    version = release(37) {
      minorApiLevel = 0
    }
  }
  defaultConfig {
    applicationId = "ee.movara.app"
    minSdk = 26
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    if (releaseSigningIsConfigured) {
      create("release") {
        storeFile = releaseKeystore
        storePassword = releaseKeystorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  // Without a keystore the release variant still builds, but comes out unsigned.
  val releaseSigningConfig = if (releaseSigningIsConfigured) {
    signingConfigs.getByName("release")
  } else {
    null
  }

  buildTypes {
    debug {
      buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
      buildConfigField("String", "TMDB_BASE_URL", "\"$stagingTmdbBaseUrl\"")
    }

    // Staging is the build the team installs and tests, and the one issue #55 shares.
    // It is shrunk exactly like release, because otherwise nobody ever runs a shrunk
    // build and a missing R8 keep rule would only surface on the release everybody
    // skips. What it keeps from debug is logging and debuggability, so a tester can
    // still read what the app is doing.
    //
    // The cost is that stack traces from a staging build are obfuscated. The mapping
    // file for them is written to build/outputs/mapping/staging/.
    //
    // initWith copies build type settings, not dependencies, so staging deliberately does
    // not get the debugImplementation Compose tooling. Staging is handed to other people
    // and should stay close to what they would install; inspect the Compose tree on a
    // debug build instead.
    create("staging") {
      initWith(getByName("debug"))
      applicationIdSuffix = ".staging"
      versionNameSuffix = "-staging"
      isDebuggable = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
      buildConfigField("String", "TMDB_BASE_URL", "\"$stagingTmdbBaseUrl\"")
    }

    release {
      isDebuggable = false
      isMinifyEnabled = true
      isShrinkResources = true
      buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
      buildConfigField("String", "TMDB_BASE_URL", "\"$productionTmdbBaseUrl\"")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = releaseSigningConfig
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }

  sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
  jvmToolchain(17)
}

// The live TMDB tests are the only tests that go out to the network. They are off unless
// somebody asks for them, so a plain `./gradlew test` does not depend on an internet
// connection, on TMDB being up, or on the request quota of whoever is running it.
//
// Switch them on for one run with `-PrunLiveTmdbTests`, or with a RUN_LIVE_TMDB_TESTS
// environment variable on a build server. Both are read through the provider API, so the
// configuration cache notices when the value changes.
fun isSwitchedOn(value: String?): Boolean {
  if (value == null) {
    return false
  }
  // A bare -PrunLiveTmdbTests has an empty value and means on.
  if (value.isBlank()) {
    return true
  }
  return value.trim().equals("true", ignoreCase = true)
}

val runLiveTmdbTests: Boolean =
  isSwitchedOn(providers.gradleProperty("runLiveTmdbTests").orNull) ||
    isSwitchedOn(providers.environmentVariable("RUN_LIVE_TMDB_TESTS").orNull)

tasks.withType<Test>().configureEach {
  systemProperty("movara.runLiveTmdbTests", runLiveTmdbTests.toString())

  // A live run answers the question "does TMDB answer right now", and a cached result from
  // an earlier run is not an answer to it.
  if (runLiveTmdbTests) {
    outputs.upToDateWhen { false }
  }
}

// Only release builds care about the keystore, so this check is not attached to preBuild.
// Developers building debug or staging should not see it at all.
val checkReleaseSigning = tasks.register("checkReleaseSigning") {
  val problem = releaseSigningProblem
  val signingIsAbsent = releaseSigningIsAbsent
  doLast {
    if (problem != null) {
      throw GradleException(problem + " See frontend/README.md section 6.")
    }
    if (signingIsAbsent) {
      logger.warn(
        "The release build is not signed. Set RELEASE_KEYSTORE_PATH, " +
          "RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS and RELEASE_KEY_PASSWORD in " +
          "local.properties (see frontend/README.md section 6). An unsigned package " +
          "cannot be installed on a device."
      )
    }
  }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
  dependsOn(checkReleaseSigning)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation.compose)

  // TMDB network layer
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.kotlinx.serialization)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.kotlinx.serialization.json)

  // Local storage
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Image loading
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)
}
