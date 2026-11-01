package ee.movara.app

import ee.movara.app.data.repository.DataRepository
import ee.movara.app.data.repository.DefaultDataRepository

/** Application-scoped dependencies. Screens receive repositories through their ViewModels. */
class AppContainer {
  val dataRepository: DataRepository by lazy { DefaultDataRepository() }
}
