
package ee.movara.app


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import ee.movara.app.ui.layout.LocalScreenLayout
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ee.movara.app.ui.theme.MovaraGold
import ee.movara.app.ui.theme.MovaraSurface
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import ee.movara.app.ui.theme.MovaraTextSecondary
import androidx.compose.ui.graphics.Color
import ee.movara.app.ui.about.AboutScreen
import ee.movara.app.ui.genres.GenresScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import ee.movara.app.ui.movies.MovieListsViewModel
import ee.movara.app.ui.movies.SavedMoviesScreen
import ee.movara.app.ui.movies.label
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import ee.movara.app.ui.home.HomeScreen
import ee.movara.app.ui.home.HomeViewModel
import ee.movara.app.ui.preferences.MoviePreferenceQuizScreen
import ee.movara.app.ui.preferences.MoviePreferenceQuizViewModel
import ee.movara.app.ui.preferences.PreferenceStartupScreen
import ee.movara.app.ui.preferences.PreferenceStartupState
import ee.movara.app.ui.preferences.PreferenceStartupViewModel
import ee.movara.app.ui.search.SearchScreen
import ee.movara.app.ui.search.SearchViewModel
import ee.movara.app.ui.selectedgenre.SelectedGenreScreen
import ee.movara.app.ui.selectedgenre.SelectedGenreViewModel
import ee.movara.app.ui.selectedgenre.SelectedGenreViewModelFactory
import ee.movara.app.ui.moviedetails.MovieDetailsRouteContent
import ee.movara.app.ui.moviedetails.MovieDetailsViewModel


// Main navigation structure of application.
// Defines the three primary destinations: Home, Genres and My Movies.
// Also provides navigation to secondary screens such as Search, Selected Genre, Movie Details and About.
@Composable
fun MainNavigation(container: AppContainer) {
  val startup = viewModel { PreferenceStartupViewModel(container.movieTasteRepository) }
  val startupState by startup.uiState.collectAsStateWithLifecycle()
  when (startupState) {
    PreferenceStartupState.LOADING, PreferenceStartupState.ERROR -> PreferenceStartupScreen(
      loading = startupState == PreferenceStartupState.LOADING,
      onRetry = startup::retry,
      onContinue = startup::enterApp
    )
    PreferenceStartupState.QUIZ -> {
      val quiz: MoviePreferenceQuizViewModel = viewModel(
        key = "firstLaunchQuiz",
        factory = viewModelFactory {
          initializer {
            MoviePreferenceQuizViewModel(
              container.movieTasteRepository, container.movieRepository, createSavedStateHandle()
            )
          }
        }
      )
      MoviePreferenceQuizScreen(
        viewModel = quiz,
        isEditing = false,
        onFinished = startup::enterApp,
        onCancel = startup::enterApp
      )
    }
    PreferenceStartupState.READY -> AppNavigation(container)
  }
}

@Composable
private fun AppNavigation(container: AppContainer) {
  val navController = rememberNavController()
  val movieLists: MovieListsViewModel = viewModel { MovieListsViewModel(container.movieRepository) }
  val listsState by movieLists.uiState.collectAsStateWithLifecycle()
  val snackbarHost = remember { SnackbarHostState() }
  LaunchedEffect(movieLists) {
    movieLists.feedback.collect { feedback ->
      val result = snackbarHost.showSnackbar(
        message = feedback.error?.let { "Could not update ${feedback.type.label}: $it" }
          ?: if (feedback.selected) "Added to ${feedback.type.label}" else "Removed from ${feedback.type.label}",
        actionLabel = if (feedback.error != null) "Retry" else if (!feedback.selected) "Undo" else null,
        withDismissAction = true,
        duration = SnackbarDuration.Short
      )
      if (result == SnackbarResult.ActionPerformed) {
        movieLists.setMembership(
          feedback.movie, feedback.type,
          if (feedback.error != null) feedback.selected else true
        )
      }
    }
  }
  val currentDestination =
    navController.currentBackStackEntryAsState().value?.destination

  val layout = LocalScreenLayout.current

  // About, Movie Details and the preference quiz are shown without the main navigation
  val hideMainNavigation =
    currentDestination?.hasRoute<AboutRoute>() == true ||
      currentDestination?.hasRoute<MovieDetailsRoute>() == true ||
      currentDestination?.hasRoute<MoviePreferencesRoute>() == true
  val showBottomBar = !hideMainNavigation && !layout.isMedium
  val showNavigationRail = !hideMainNavigation && layout.isMedium

  // Shared layout: bottom navigation bar on compact screens, navigation rail on medium
  Scaffold(
    snackbarHost = {
      // Keep the snackbar next to the rail, not on top of it
      val snackbarStart = if (showNavigationRail) NavigationRailWidth else 0.dp
      SnackbarHost(snackbarHost, modifier = Modifier.padding(start = snackbarStart))
    },
    bottomBar = {
      if (showBottomBar) {
        NavigationBar(
          modifier = Modifier.height(80.dp),
          containerColor = MovaraSurface
        ) {
          for (tab in mainTabs) {
            val selected = tab.isSelected(currentDestination)
            NavigationBarItem(
              selected = selected,
              onClick = { navController.openTab(tab.route) },
              icon = { MainTabIcon(tab, selected) },
              label = { Text(tab.label) },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MovaraGold,
                selectedTextColor = MovaraGold,
                indicatorColor = Color.Transparent,
                unselectedIconColor = MovaraTextSecondary,
                unselectedTextColor = MovaraTextSecondary
              )
            )
          }
        }
      }
    }
  ) { padding ->
    // The rail reaches the screen edges and pads itself against the system bars, so the
    // content next to it must not repeat the padding on the rail's side.
    val layoutDirection = LocalLayoutDirection.current
    val contentPadding = if (showNavigationRail) {
      PaddingValues(
        top = padding.calculateTopPadding(),
        end = padding.calculateEndPadding(layoutDirection),
        bottom = padding.calculateBottomPadding()
      )
    } else {
      padding
    }

    Row(modifier = Modifier.fillMaxSize()) {
      if (showNavigationRail) {
        NavigationRail(containerColor = MovaraSurface) {
          for (tab in mainTabs) {
            val selected = tab.isSelected(currentDestination)
            NavigationRailItem(
              selected = selected,
              onClick = { navController.openTab(tab.route) },
              icon = { MainTabIcon(tab, selected) },
              label = { Text(tab.label) },
              colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MovaraGold,
                selectedTextColor = MovaraGold,
                indicatorColor = Color.Transparent,
                unselectedIconColor = MovaraTextSecondary,
                unselectedTextColor = MovaraTextSecondary
              )
            )
          }
        }
      }

      // Register all navigation destinations and their screen content
      NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = Modifier
          .weight(1f)
          .padding(contentPadding)
          // The rail already covers the start inset, so all of it counts as handled
          .consumeWindowInsets(padding)
      ) {

        composable<HomeRoute> {
          val homeViewModel = viewModel {
            HomeViewModel(container.movieRepository, container.movieTasteRepository, movieLists.store)
          }
          HomeScreen(
            viewModel = homeViewModel,
            onSearch = { navController.navigate(SearchRoute) },
            onMovieClick = { movieId -> navController.navigate(MovieDetailsRoute(movieId)) },
            onEditPreferences = { navController.navigate(MoviePreferencesRoute) }
          )
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
          SavedMoviesScreen(
            state = listsState,
            onMembershipChange = movieLists::setMembership,
            onOpenMovie = { navController.navigate(MovieDetailsRoute(it)) },
            onAbout = { navController.navigate(AboutRoute) },
            onRetry = movieLists::retry
          )
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



        composable<MovieDetailsRoute> { backStackEntry ->
          val route = backStackEntry.toRoute<MovieDetailsRoute>()

          val movieDetailsViewModel: MovieDetailsViewModel = viewModel(
            viewModelStoreOwner = backStackEntry
          ) {
            MovieDetailsViewModel(
              movieId = route.movieId,
              repository = container.movieRepository,
              movieLists = movieLists.store
            )
          }

          MovieDetailsRouteContent(
            viewModel = movieDetailsViewModel,
            onBack = {
              navController.popBackStack()
            }
          )
        }


        composable<MoviePreferencesRoute> {
          val quiz: MoviePreferenceQuizViewModel = viewModel(
            factory = viewModelFactory {
              initializer {
                MoviePreferenceQuizViewModel(
                  container.movieTasteRepository, container.movieRepository, createSavedStateHandle()
                )
              }
            }
          )
          MoviePreferenceQuizScreen(
            viewModel = quiz,
            isEditing = true,
            onFinished = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
          )
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

// One main destination, shown in the bottom navigation bar and in the navigation rail
private class MainTab(
  val label: String,
  val outlinedIcon: Int,
  val filledIcon: Int,
  val route: Any,
  val isSelected: (NavDestination?) -> Boolean
)

// The Material 3 rail is 80 dp wide, as in the design specs
private val NavigationRailWidth = 80.dp

private val mainTabs = listOf(
  MainTab(
    label = "Home",
    outlinedIcon = R.drawable.ic_nav_home_outline,
    filledIcon = R.drawable.ic_nav_home_filled,
    route = HomeRoute,
    isSelected = { destination -> destination?.hasRoute<HomeRoute>() == true }
  ),
  MainTab(
    label = "Genres",
    outlinedIcon = R.drawable.ic_nav_style_outline,
    filledIcon = R.drawable.ic_nav_style_filled,
    route = GenresRoute,
    isSelected = { destination -> destination?.hasRoute<GenresRoute>() == true }
  ),
  MainTab(
    label = "My Movies",
    outlinedIcon = R.drawable.ic_nav_person_outline,
    filledIcon = R.drawable.ic_nav_person_filled,
    route = MyMoviesRoute,
    isSelected = { destination -> destination?.hasRoute<MyMoviesRoute>() == true }
  )
)

// Icon shared by both navigation components: filled with a small gold dot when selected
@Composable
private fun MainTabIcon(tab: MainTab, selected: Boolean) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    Icon(
      painter = painterResource(
        id = if (selected) tab.filledIcon else tab.outlinedIcon
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
}
