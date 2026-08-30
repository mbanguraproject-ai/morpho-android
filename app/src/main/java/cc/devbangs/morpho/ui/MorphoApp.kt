package cc.devbangs.morpho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cc.devbangs.morpho.core.Dest
import cc.devbangs.morpho.data.Workspace
import cc.devbangs.morpho.ui.onboarding.OnboardingScreen
import cc.devbangs.morpho.ui.category.CategoryScreen
import cc.devbangs.morpho.ui.components.MorphoBottomBar
import cc.devbangs.morpho.ui.home.HomeScreen
import cc.devbangs.morpho.ui.search.SearchScreen
import cc.devbangs.morpho.ui.settings.SettingsScreen
import cc.devbangs.morpho.ui.plus.PlusScreen
import cc.devbangs.morpho.ui.tool.ToolScreen
import cc.devbangs.morpho.ui.workspace.AddToolsScreen
import cc.devbangs.morpho.ui.workspace.ArrangeWorkspaceScreen
import cc.devbangs.morpho.ui.theme.Paper
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.runtime.remember

@Composable
fun MorphoApp() {
    if (!Workspace.onboarded) {
        OnboardingScreen(onDone = { Workspace.completeOnboarding() })
        return
    }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    val topLevel = route == Dest.Home.route || route == Dest.Categories.route
    val barHaze = remember { HazeState() }
    val currentTab = when (route) {
        Dest.Categories.route -> "categories"
        else -> "home"
    }

    Box(Modifier.fillMaxSize().background(Paper)) {
        NavHost(
            navController = nav,
            startDestination = Dest.Home.route,
            modifier = Modifier.fillMaxSize().haze(barHaze),
            enterTransition = {
                slideInHorizontally(tween(320)) { it / 5 } + fadeIn(tween(320))
            },
            exitTransition = {
                slideOutHorizontally(tween(320)) { -it / 8 } + fadeOut(tween(220))
            },
            popEnterTransition = {
                slideInHorizontally(tween(320)) { -it / 8 } + fadeIn(tween(320))
            },
            popExitTransition = {
                slideOutHorizontally(tween(320)) { it / 5 } + fadeOut(tween(220))
            }
        ) {
            composable(Dest.Home.route) {
                HomeScreen(
                    onOpenTool = { nav.navigate(Dest.Tool(it).route) },
                    onOpenSettings = { nav.navigate(Dest.Settings.route) },
                    onOpenAddTools = { nav.navigate(Dest.AddTools.route) },
                    onArrange = { nav.navigate(Dest.Arrange.route) },
                    onExploreTools = {
                        nav.navigate(Dest.Categories.route) {
                            popUpTo(Dest.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    contentPadding = bottomBarPadding(topLevel)
                )
            }
            composable(Dest.Categories.route) {
                CategoriesScreen(
                    onOpenCategory = { nav.navigate(Dest.Category(it).route) },
                    contentPadding = bottomBarPadding(true)
                )
            }
            composable(
                Dest.Category.route,
                arguments = listOf(navArgument(Dest.Category.ARG) { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString(Dest.Category.ARG).orEmpty()
                CategoryScreen(
                    categoryId = id,
                    onBack = { nav.popBackStack() },
                    onOpenTool = { nav.navigate(Dest.Tool(it).route) },
                    contentPadding = bottomBarPadding(false)
                )
            }
            composable(
                Dest.Tool.route,
                arguments = listOf(navArgument(Dest.Tool.ARG) { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString(Dest.Tool.ARG).orEmpty()
                ToolScreen(
                    toolId = id,
                    onBack = { nav.popBackStack() },
                    onOpenTool = { nav.navigate(Dest.Tool(it).route) },
                    onOpenPlus = { nav.navigate(Dest.Plus.route) },
                    contentPadding = PaddingValues()
                )
            }
            composable(Dest.Search.route) {
                SearchScreen(
                    onBack = { nav.popBackStack() },
                    onOpenTool = { nav.navigate(Dest.Tool(it).route) },
                    contentPadding = bottomBarPadding(false)
                )
            }
            composable(Dest.Settings.route) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPlus = { nav.navigate(Dest.Plus.route) },
                    contentPadding = bottomBarPadding(false)
                )
            }
            composable(Dest.Plus.route) {
                PlusScreen(
                    onBack = { nav.popBackStack() },
                    contentPadding = bottomBarPadding(false)
                )
            }
            composable(Dest.AddTools.route) {
                AddToolsScreen(
                    onBack = { nav.popBackStack() },
                    contentPadding = bottomBarPadding(false)
                )
            }
            composable(Dest.Arrange.route) {
                ArrangeWorkspaceScreen(
                    onBack = { nav.popBackStack() },
                    contentPadding = bottomBarPadding(false)
                )
            }
        }

        if (topLevel) {
            MorphoBottomBar(
                hazeState = barHaze,
                current = currentTab,
                onSelect = { tab ->
                    val dest = if (tab == "categories") Dest.Categories.route
                    else if (tab == "search") Dest.Search.route
                    else Dest.Home.route
                    if (tab == "search") nav.navigate(Dest.Search.route)
                    else nav.navigate(dest) {
                        popUpTo(Dest.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Bottom content inset for scrollable screens.
 *
 * [withBar] is true only for top-level screens that sit beneath the floating
 * bottom bar; those reserve the bar height on top of the system inset.
 *
 * Every screen gets the navigation-bar inset so the final row can always be
 * scrolled clear of Android system navigation (gesture and 3-button alike).
 * The inset belongs to the scrollable content, not to a permanent footer.
 */
@Composable
private fun bottomBarPadding(withBar: Boolean): PaddingValues {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(bottom = if (withBar) 92.dp + navInset else navInset)
}
