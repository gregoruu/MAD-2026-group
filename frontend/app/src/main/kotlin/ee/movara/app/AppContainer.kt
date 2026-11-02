package ee.movara.app

import android.content.Context
import androidx.room.Room
import ee.movara.app.data.local.MovaraDatabase
import ee.movara.app.data.repository.DataRepository
import ee.movara.app.data.repository.DefaultDataRepository

/** Application-scoped dependencies. Screens receive repositories through their ViewModels. */
class AppContainer(private val context: Context) {

  val database: MovaraDatabase by lazy {
    Room.databaseBuilder(
      context.applicationContext,
      MovaraDatabase::class.java,
      "movara.db"
    ).build()
  }
  val dataRepository: DataRepository by lazy { DefaultDataRepository() }
}
