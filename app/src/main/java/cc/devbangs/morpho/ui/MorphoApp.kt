package cc.devbangs.morpho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cc.devbangs.morpho.core.Dest
import cc.devbangs.morpho.ui.category.CategoryScreen
import cc.devbangs.morpho.ui.components.MorphoBottomBar
import cc.devbangs.morpho.ui.home.HomeScreen
import cc.devbangs.morpho.ui.search.SearchScreen
import cc.devbangs.morpho.ui.tool.ToolScreen
import cc.devbangs.morpho.ui.theme.Paper

@Composable
fun MorphoApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    val topLevel = route == Dest.Home.route || route == Dest.Categories.route
    val currentTab = when (route) {
        Dest.Categories.route -> "categories"
        else -> "home"
    }

    Box(Modifier.fillMaxSize().background(Paper)) {
        NavHost(
            navController = nav,
            startDestination = Dest.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Dest.Home.route) {
                HomeScreen(
                    onOpenTool = { nav.navigate(Dest.Tool(it).route) },
                    onOpenCategory = { nav.navigate(Dest.Category(it).route) },
                    onOpenSearch = { nav.navigate(Dest.Search.route) },
                    onSeeAllCategories = {
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
                    contentPadding = PaddingValues()
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
                    contentPadding = PaddingValues()
                )
            }
            composable(Dest.Search.route) {
                SearchScreen(
                    onBack = { nav.popBackStack() },
                    onOpenTool = { nav.navigate(Dest.Tool(it).route) },
                    contentPadding = PaddingValues()
                )
            }
        }

        if (topLevel) {
            MorphoBottomBar(
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

/** Reserve space for the bottom bar (58dp) + nav inset on top-level screens. */
@Composable
private fun bottomBarPadding(show: Boolean): PaddingValues =
    PaddingValues(
        bottom = if (show) 92.dp + WindowInsets.navigationBars
            .asPaddingValues().calculateBottomPadding() else 0.dp
    )
