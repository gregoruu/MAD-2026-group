package ee.movara.app

import android.app.Application

class MovaraApplication : Application() {
  val container: AppContainer by lazy { AppContainer() }
}
