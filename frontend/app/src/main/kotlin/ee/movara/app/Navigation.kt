
package ee.movara.app


import androidx.compose.foundation.layout.RowScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ee.movara.app.ui.theme.MovaraGold
import ee.movara.app.ui.theme.MovaraSurface
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import ee.movara.app.ui.theme.MovaraTextSecondary
import androidx.compose.ui.graphics.Color

@Composable
fun MainNavigation(container: AppContainer) {
  val navController = rememberNavController()
  val currentDestination =
    navController.currentBackStackEntryAsState().value?.destination

  Scaffold(
    bottomBar = {
      NavigationBar(
        modifier = Modifier.height(80.dp),
        containerColor = MovaraSurface
      ) {

        MainTabItem(
          label = "Home",
          outlinedIcon = R.drawable.ic_nav_home_outline,
          filledIcon = R.drawable.ic_nav_home_filled,
          selected = currentDestination?.hasRoute<HomeRoute>() == true,
          onClick = { navController.openTab(HomeRoute) }
        )

        MainTabItem(
          label = "Genres",
          outlinedIcon = R.drawable.ic_nav_style_outline,
          filledIcon = R.drawable.ic_nav_style_filled,
          selected = currentDestination?.hasRoute<GenresRoute>() == true,
          onClick = { navController.openTab(GenresRoute) }
        )

        MainTabItem(
          label = "My Movies",
          outlinedIcon = R.drawable.ic_nav_person_outline,
          filledIcon = R.drawable.ic_nav_person_filled,
          selected = currentDestination?.hasRoute<MyMoviesRoute>() == true,
          onClick = { navController.openTab(MyMoviesRoute) }
        )

      }
    }
  ) { padding ->
    NavHost(
      navController = navController,
      startDestination = HomeRoute,
      modifier = Modifier.padding(padding)
    ) {

      composable<HomeRoute> {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text("Home", fontSize = 26.sp)

          Button(
            onClick = {
              navController.navigate(SearchRoute)
            }
          ) {
            Text("Open Search")
          }
        }
      }

      composable<SearchRoute> {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text("Search", fontSize = 26.sp)

          Button(
            onClick = {
              navController.popBackStack()
            }
          ) {
            Text("Back")
          }
        }
      }

      composable<GenresRoute> {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text("Genres", fontSize = 26.sp)

          Button(
            onClick = {
              navController.navigate(SelectedGenreRoute(28))
            }
          ) {
            Text("Open Action genre")
          }
        }
      }

      composable<MyMoviesRoute> {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text("My Movies", fontSize = 26.sp)

          Button(
            onClick = {
              navController.navigate(AboutRoute)
            }
          ) {
            Text("Open About")
          }
        }
      }

      composable<SelectedGenreRoute> {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text("Selected Genre", fontSize = 26.sp)

          Button(
            onClick = {
              navController.navigate(MovieDetailsRoute(1))
            }
          ) {
            Text("Open Movie Details")
          }

          Button(
            onClick = { navController.popBackStack() }
          ) {
            Text("Back")
          }
        }
      }

      composable<MovieDetailsRoute> {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text("Movie Details", fontSize = 26.sp)

          Button(
            onClick = { navController.popBackStack() }
          ) {
            Text("Back")
          }
        }
      }

      composable<AboutRoute> {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text("About", fontSize = 26.sp)

          Button(
            onClick = { navController.popBackStack() }
          ) {
            Text("Back")
          }
        }
      }

    }
  }
}

private fun <T : Any> NavHostController.openTab(route: T) {
  navigate(route) {
    popUpTo(graph.findStartDestination().id) {
      saveState = true
    }
    launchSingleTop = true
    restoreState = true
  }
}


@Composable
private fun RowScope.MainTabItem(
  label: String,
  outlinedIcon: Int,
  filledIcon: Int,
  selected: Boolean,
  onClick: () -> Unit
) {
  NavigationBarItem(
    selected = selected,
    onClick = onClick,
    icon = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        Icon(
          painter = painterResource(
            id = if (selected) filledIcon else outlinedIcon
          ),
          contentDescription = null,
          modifier = Modifier.size(24.dp)
        )

        if (selected) {
          Box(
            modifier = Modifier
              .size(4.dp)
              .background(MovaraGold, CircleShape)
          )
        }
      }
    },
    label = { Text(label) },
    colors = NavigationBarItemDefaults.colors(
      selectedIconColor = MovaraGold,
      selectedTextColor = MovaraGold,
      indicatorColor = Color.Transparent,
      unselectedIconColor = MovaraTextSecondary,
      unselectedTextColor = MovaraTextSecondary
    )
  )
}


@Composable
private fun PlaceholderScreen(title: String) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text(text = title, fontSize = 26.sp)
  }
}
