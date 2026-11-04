package ee.movara.app

import android.content.Context
import androidx.room.Room
import ee.movara.app.data.local.MovaraDatabase
import ee.movara.app.data.remote.TmdbApiService
import ee.movara.app.data.remote.createTmdbApiService
import ee.movara.app.data.remote.createTmdbOkHttpClient
import ee.movara.app.data.repository.DataRepository
import ee.movara.app.data.repository.DefaultDataRepository
import okhttp3.OkHttpClient

/** Application-scoped dependencies. Screens receive repositories through their ViewModels. */
class AppContainer(private val context: Context) {

  val database: MovaraDatabase by lazy {
    Room.databaseBuilder(
      context.applicationContext,
      MovaraDatabase::class.java,
      "movara.db"
    ).build()
  }

  /** The one HTTP client of the app. Everything that talks to TMDB shares it. */
  private val okHttpClient: OkHttpClient by lazy {
    createTmdbOkHttpClient(
      apiKey = BuildConfig.TMDB_API_KEY,
      enableHttpLogging = BuildConfig.ENABLE_HTTP_LOGGING
    )
  }

  val tmdbApiService: TmdbApiService by lazy { createTmdbApiService(okHttpClient) }

  val dataRepository: DataRepository by lazy { DefaultDataRepository() }
}
