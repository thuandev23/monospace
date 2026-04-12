package com.monospace.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.monospace.app.feature.launcher.HomeScreen
import com.monospace.app.feature.settings.SettingsScreen
import com.monospace.app.feature.tasks.TaskListScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Settings : Screen("settings")
}

@Composable
fun MonospaceNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            // OnboardingScreen()
        }
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Tasks.route) {
            TaskListScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
