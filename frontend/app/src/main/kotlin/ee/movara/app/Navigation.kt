package ee.movara.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ee.movara.app.ui.main.MainScreen

@Composable
fun MainNavigation() {
  val navController = rememberNavController()

  // Only the starter screen for now; the real destinations are added in issue #30.
  NavHost(navController = navController, startDestination = Main) {
    composable<Main> { MainScreen() }
  }
}
