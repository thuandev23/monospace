package com.monospace.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.feature.detail.TaskDetailScreen
import com.monospace.app.feature.focus.DetoxStatsScreen
import com.monospace.app.feature.focus.FocusScreen
import com.monospace.app.feature.launcher.HomeScreen
import com.monospace.app.feature.onboardings.OnboardingScreen
import com.monospace.app.feature.onboardings.OnboardingViewModel
import com.monospace.app.feature.search.SearchScreen
import com.monospace.app.feature.paywall.ProUpgradeScreen
import com.monospace.app.feature.settings.AboutScreen
import com.monospace.app.feature.settings.GeneralSettingsScreen
import com.monospace.app.feature.settings.NotionIntegrationScreen
import com.monospace.app.feature.settings.RemindersIntegrationScreen
import com.monospace.app.feature.settings.SettingsScreen
import com.monospace.app.feature.settings.TabBarSettingsScreen
import com.monospace.app.feature.settings.TaskDefaultScreen
import com.monospace.app.feature.settings.WallpaperScreen
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
    object Focus : Screen("focus")
    object Search : Screen("search")
    object TaskDefault : Screen("task_default")
    object GeneralSettings : Screen("general_settings")
    object Wallpaper : Screen("wallpaper")
    object About : Screen("about")
    object TabBarSettings : Screen("tab_bar_settings")
    object ProUpgrade : Screen("pro_upgrade")
    object NotionIntegration : Screen("notion_integration")
    object DetoxStats : Screen("detox_stats")
    object RemindersIntegration : Screen("reminders_integration")
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
            val viewModel: OnboardingViewModel = hiltViewModel()
            OnboardingScreen(
                onFinish = {
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Home.BASE) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
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
                },
                onTodayClick = {
                    // "today" listId → GetTasksUseCase trả tasks hôm nay từ tất cả lists
                    navController.navigate(Screen.Home.withList("today")) {
                        launchSingleTop = true
                    }
                },
                onUpcomingClick = {
                    navController.navigate(Screen.Upcoming.route) {
                        launchSingleTop = true
                    }
                },
                onAllClick = {
                    navController.navigate(Screen.Home.withList("all")) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToFocus = { navController.navigate(Screen.Focus.route) },
                onNavigateToLists = { navController.navigate(Screen.Tasks.route) },
                onNavigateToTaskDefault = { navController.navigate(Screen.TaskDefault.route) },
                onNavigateToGeneral = { navController.navigate(Screen.GeneralSettings.route) },
                onNavigateToWallpaper = { navController.navigate(Screen.Wallpaper.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToTabBar = { navController.navigate(Screen.TabBarSettings.route) },
                onNavigateToNotion = { navController.navigate(Screen.NotionIntegration.route) },
                onNavigateToReminders = { navController.navigate(Screen.RemindersIntegration.route) },
                onNavigateToProUpgrade = { navController.navigate(Screen.ProUpgrade.route) }
            )
        }
        composable(Screen.TaskDefault.route) {
            TaskDefaultScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.GeneralSettings.route) {
            GeneralSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Wallpaper.route) {
            WallpaperScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.TabBarSettings.route) {
            TabBarSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.ProUpgrade.route) {
            ProUpgradeScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.NotionIntegration.route) {
            NotionIntegrationScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.RemindersIntegration.route) {
            RemindersIntegrationScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Focus.route) {
            FocusScreen(
                onNavigateToDetoxStats = { navController.navigate(Screen.DetoxStats.route) }
            )
        }
        composable(Screen.DetoxStats.route) {
            DetoxStatsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToTask = { taskId ->
                    navController.navigate(Screen.TaskDetail.withId(taskId))
                },
                onNavigateToCreateTask = {
                    navController.navigate(Screen.Home.BASE)
                },
                onClose = { navController.popBackStack() }
            )
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
