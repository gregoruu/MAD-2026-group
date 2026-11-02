import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

// The TMDB API key is never committed. Developers put it in the ignored local.properties
// file at the repository root; build servers pass it as the TMDB_API_KEY environment
// variable. Both sources are read through the provider API, so the configuration cache
// is invalidated when the key changes.
val localPropertiesText: String? = providers.fileContents(
  isolated.rootProject.projectDirectory.file("local.properties")
).asText.orNull

val keyFromLocalProperties: String = if (localPropertiesText == null) {
  ""
} else {
  val properties = Properties()
  properties.load(localPropertiesText.reader())
  properties.getProperty("TMDB_API_KEY").orEmpty().trim()
}

val keyFromEnvironment: String = providers.environmentVariable("TMDB_API_KEY").orNull.orEmpty().trim()

val tmdbApiKey: String = if (keyFromLocalProperties.isNotEmpty()) {
  keyFromLocalProperties
} else {
  keyFromEnvironment
}

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

    // buildConfigField writes the value straight into generated Java source,
    // so the key is escaped and quoted here.
    val escapedTmdbApiKey = tmdbApiKey
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
    buildConfigField("String", "TMDB_API_KEY", "\"$escapedTmdbApiKey\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

// The warning lives in a task action, not at configuration time. A configuration-time
// warning is only printed on the first build and is then swallowed by the configuration
// cache, which would let a developer believe the key is configured when it is not.
val warnAboutMissingTmdbApiKey = tasks.register("warnAboutMissingTmdbApiKey") {
  val keyIsMissing = tmdbApiKey.isEmpty()
  doLast {
    if (keyIsMissing) {
      logger.warn(
        "TMDB_API_KEY is not set. Add it to local.properties at the repository root " +
          "(see frontend/README.md). Requests to TMDB will fail without it."
      )
    }
  }
}

tasks.named("preBuild") {
  dependsOn(warnAboutMissingTmdbApiKey)
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
