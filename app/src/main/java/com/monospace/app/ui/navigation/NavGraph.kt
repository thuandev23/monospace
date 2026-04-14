package com.monospace.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.monospace.app.feature.detail.TaskDetailScreen
import com.monospace.app.feature.launcher.HomeScreen
import com.monospace.app.feature.settings.SettingsScreen
import com.monospace.app.feature.tasks.TaskListScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home?listId={listId}") {
        fun withList(listId: String) = "home?listId=$listId"
        const val BASE = "home"
    }
    object Tasks : Screen("tasks")
    object Settings : Screen("settings")
    object TaskDetail : Screen("task/{taskId}") {
        fun withId(taskId: String) = "task/$taskId"
    }
}

@Composable
fun MonospaceNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.BASE
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            // OnboardingScreen()
        }
        composable(
            route = Screen.Home.route,
            arguments = listOf(
                navArgument("listId") {
                    type = NavType.StringType
                    defaultValue = "default"
                }
            )
        ) {
            HomeScreen(
                onNavigateToTask = { taskId ->
                    navController.navigate(Screen.TaskDetail.withId(taskId))
                }
            )
        }
        composable(Screen.Tasks.route) {
            TaskListScreen(
                onListClick = { listId ->
                    navController.navigate(Screen.Home.withList(listId)) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType }
            )
        ) {
            TaskDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
