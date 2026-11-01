package ee.movara.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import ee.movara.app.ui.main.MainScreenViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import ee.movara.app.ui.main.MainScreen

@Composable
fun MainNavigation(container: AppContainer) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          val viewModel: MainScreenViewModel = viewModel {
            MainScreenViewModel(container.dataRepository)
          }
          MainScreen(viewModel = viewModel)
        }
      },
  )
}
