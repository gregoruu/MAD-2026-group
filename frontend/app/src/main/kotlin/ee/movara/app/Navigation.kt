package ee.movara.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ee.movara.app.ui.main.MainScreen
import ee.movara.app.ui.main.MainScreenViewModel

@Composable
fun MainNavigation(container: AppContainer) {
  val navController = rememberNavController()

  // Only the starter screen for now; the real destinations are added in issue #30.
  NavHost(navController = navController, startDestination = Main) {
    composable<Main> {
      val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(container.dataRepository)
      }
      MainScreen(viewModel = viewModel)
    }
  }
}
