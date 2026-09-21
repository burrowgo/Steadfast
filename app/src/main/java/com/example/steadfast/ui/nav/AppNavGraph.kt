package com.example.steadfast.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.steadfast.R
import com.example.steadfast.ui.history.HistoryScreen
import com.example.steadfast.ui.home.HomeScreen
import com.example.steadfast.ui.ranks.RanksScreen
import com.example.steadfast.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Ranks : Screen("ranks")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val titleRes: Int,
    val iconRes: Int
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, R.string.nav_home, R.drawable.ic_nav_home),
    BottomNavItem(Screen.Ranks, R.string.nav_ranks, R.drawable.ic_nav_ranks),
    BottomNavItem(Screen.History, R.string.nav_history, R.drawable.ic_nav_history)
)

@Composable
fun MainApp(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelDestination = bottomNavItems.any { it.screen.route == currentRoute }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = stringResource(id = item.titleRes)
                                )
                            },
                            label = { Text(stringResource(id = item.titleRes)) },
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.widthIn(max = 600.dp)) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    enterTransition = { fadeIn(tween(200)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    }
                    composable(Screen.Ranks.route) {
                        RanksScreen()
                    }
                    composable(Screen.History.route) {
                        HistoryScreen()
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
