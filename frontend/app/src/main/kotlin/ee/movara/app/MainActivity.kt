package ee.movara.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import ee.movara.app.ui.layout.LocalScreenLayout
import ee.movara.app.ui.layout.screenLayoutFor
import ee.movara.app.ui.theme.MovaraTheme

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    setContent {
      // Recalculated when the window changes, for example on rotation or unfolding.
      val windowSizeClass = calculateWindowSizeClass(this)
      val screenLayout = screenLayoutFor(windowSizeClass.widthSizeClass)
      MovaraTheme {
        CompositionLocalProvider(LocalScreenLayout provides screenLayout) {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            MainNavigation((application as MovaraApplication).container)
          }
        }
      }
    }
  }
}
