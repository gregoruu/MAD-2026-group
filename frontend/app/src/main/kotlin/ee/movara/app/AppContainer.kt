package ee.movara.app

import android.content.Context
import ee.movara.app.data.repository.DataRepository
import ee.movara.app.data.repository.DefaultDataRepository

/** Application-scoped dependencies. Screens receive repositories through their ViewModels. */
class AppContainer(private val context: Context) {
  val dataRepository: DataRepository by lazy { DefaultDataRepository() }
}
