package com.monospace.app.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
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
import com.monospace.app.feature.upcoming.UpcomingScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home?listId={listId}&showSearch={showSearch}") {
        fun withList(listId: String) = "home?listId=$listId"
        fun withSearch() = "home?showSearch=true"
        const val BASE = "home"
    }
    object Upcoming : Screen("upcoming")
    object Tasks : Screen("tasks")
    object Settings : Screen("settings")
    object TaskDetail : Screen("task/{taskId}") {
        fun withId(taskId: String) = "task/$taskId"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
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
                },
                navArgument("showSearch") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val showSearch = backStackEntry.arguments?.getBoolean("showSearch") ?: false
            HomeScreen(
                initialShowSearch = showSearch,
                onNavigateToTask = { taskId ->
                    navController.navigate(Screen.TaskDetail.withId(taskId))
                },
                onNavigateToLists = {
                    navController.navigate(Screen.Tasks.route)
                }
            )
        }
        composable(Screen.Upcoming.route) {
            UpcomingScreen(
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
