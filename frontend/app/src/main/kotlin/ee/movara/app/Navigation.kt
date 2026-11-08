
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
import ee.movara.app.ui.about.AboutScreen
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import ee.movara.app.ui.genres.GenresScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import ee.movara.app.ui.search.SearchScreen
import ee.movara.app.ui.search.SearchViewModel
import ee.movara.app.ui.selectedgenre.SelectedGenreScreen
import ee.movara.app.ui.selectedgenre.SelectedGenreViewModel
import ee.movara.app.ui.selectedgenre.SelectedGenreViewModelFactory

// Main navigation structure of application.
// Defines the three primary destinations: Home, Genres and My Movies.
// Also provides navigation to secondary screens such as Search, Selected Genre, Movie Details and About.
@Composable
fun MainNavigation(container: AppContainer) {
  val navController = rememberNavController()
  val currentDestination =
    navController.currentBackStackEntryAsState().value?.destination

//Shared layout and bottom navigation bar
  Scaffold(
    bottomBar = {
      if (currentDestination?.hasRoute<AboutRoute>() != true) {
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
    }
    // Register all navigation destinations and their screen content
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
        val searchViewModel: SearchViewModel = viewModel(
          factory = viewModelFactory {
            initializer {
              SearchViewModel(container.movieRepository, createSavedStateHandle())
            }
          }
        )
        val state by searchViewModel.uiState.collectAsStateWithLifecycle()
        SearchScreen(
          state = state,
          onQueryChange = searchViewModel::onQueryChange,
          onSearch = searchViewModel::submitSearch,
          onLoadMore = searchViewModel::loadMore,
          onRetry = searchViewModel::retry,
          onMovieClick = { navController.navigate(MovieDetailsRoute(it)) },
          onBack = { navController.popBackStack() }
        )
      }

      composable<GenresRoute> {
        GenresScreen(
          onGenreClick = { genreId ->
            navController.navigate(
              SelectedGenreRoute(genreId)
            )
          }
        )
      }

      composable<MyMoviesRoute> {
        Box(
          modifier = Modifier.fillMaxSize()
        ) {
          // Info button opens the About screen
          IconButton(
            onClick = {
              navController.navigate(AboutRoute)
            },
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp)
              .size(48.dp)
          ) {
            Icon(
              painter = painterResource(R.drawable.ic_ui_info),
              contentDescription = stringResource(R.string.about_open),
              tint = MovaraGold
            )
          }

          Text(
            text = "My Movies",
            fontSize = 26.sp,
            modifier = Modifier.align(Alignment.Center)
          )
        }
      }


      composable<SelectedGenreRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<SelectedGenreRoute>()

        val selectedGenreViewModel: SelectedGenreViewModel = viewModel(
          viewModelStoreOwner = backStackEntry,
          factory = SelectedGenreViewModelFactory(
            genreId = route.genreId,
            movieRepository = container.movieRepository
          )
        )

        // Observe changes in loading
        val uiState = selectedGenreViewModel.uiState
          .collectAsStateWithLifecycle().value

        SelectedGenreScreen(
          genreId = route.genreId,
          movies = uiState.movies,
          isLoading = uiState.isLoading,
          isLoadingMore = uiState.isLoadingMore,
          errorMessage = uiState.errorMessage,
          loadMoreError = uiState.loadMoreError,
          hasMore = uiState.hasMore,
          onRetry = selectedGenreViewModel::retry,
          onLoadMore = selectedGenreViewModel::loadMore,
          onBack = {
            navController.popBackStack()
          },
          onMovieClick = { movie ->
            navController.navigate(
              MovieDetailsRoute(movie.id)
            )
          }
        )
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
        AboutScreen(
          onBack = {
            navController.popBackStack()
          }
        )
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

// Reusable bottom navigation item
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
