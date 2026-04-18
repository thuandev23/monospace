package com.monospace.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.monospace.app.feature.blocking.BlockedAppOverlay
import com.monospace.app.feature.focus.FocusViewModel
import com.monospace.app.feature.lock.AppLockScreen
import com.monospace.app.feature.launcher.components.HomeBottomBar
import com.monospace.app.feature.onboardings.OnboardingViewModel
import com.monospace.app.feature.settings.GeneralSettingsViewModel
import com.monospace.app.feature.settings.TabBarSettingsViewModel
import com.monospace.app.ui.navigation.MonospaceNavGraph
import com.monospace.app.ui.navigation.Screen
import com.monospace.app.ui.theme.FocusTheme
import com.monospace.app.ui.theme.MONOSPACETheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainScreen() {
    val generalViewModel: GeneralSettingsViewModel = hiltViewModel()
    val generalSettings by generalViewModel.settings.collectAsState()
    val lockPin by generalViewModel.lockPin.collectAsState()

    var isLocked by remember { mutableStateOf(lockPin != null) }

    // Re-lock when app resumes from background (if PIN is set)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (lockPin != null) isLocked = true
        }
    }

    // When PIN is removed via settings, auto-unlock
    LaunchedEffect(lockPin) {
        if (lockPin == null) isLocked = false
    }

    MONOSPACETheme(appTheme = generalSettings.theme) {

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
    val isOnline by connectivityViewModel.isOnline.collectAsState()

    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsState()

    val tabBarViewModel: TabBarSettingsViewModel = hiltViewModel()
    val tabBarSettings by tabBarViewModel.settings.collectAsState()

    val focusViewModel: FocusViewModel = hiltViewModel()
    val blockedPackage by focusViewModel.blockedPackage.collectAsState()
    val focusUiState by focusViewModel.uiState.collectAsState()
    val timerState by focusViewModel.timerState.collectAsState()

    // Tự động refresh quyền khi quay lại app
    LaunchedEffect(Unit) {
        focusViewModel.refreshPermissions()
    }

    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted == false) {
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(Screen.Home.BASE) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Upcoming.route,
        Screen.Tasks.route,
        Screen.Settings.route,
        Screen.Search.route
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    Box(
                        modifier = Modifier.padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HomeBottomBar(
                            showUpcoming = tabBarSettings.showUpcoming,
                            showSearch = tabBarSettings.showSearch,
                            onTodayClick = {
                                navController.navigate(Screen.Home.BASE) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                        saveState = false
                                    }
                                    launchSingleTop = false
                                }
                            },
                            onSearchClick = {
                                navController.navigate(Screen.Search.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onUpcomingClick = {
                                navController.navigate(Screen.Upcoming.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onSettingsClick = {
                                navController.navigate(Screen.Settings.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                AnimatedVisibility(
                    visible = !isOnline,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Text(
                        text = "Không có kết nối mạng — đang lưu offline",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FocusTheme.colors.secondary)
                            .padding(vertical = 6.dp, horizontal = 16.dp),
                        style = FocusTheme.typography.label.copy(
                            color = FocusTheme.colors.background,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    MonospaceNavGraph(navController = navController)
                }
            }
        }

        // ĐÂY LÀ PHẦN QUAN TRỌNG: Overlay chặn app luôn nằm trên cùng
        BlockedAppOverlay(
            blockedPackage = blockedPackage,
            activeProfileName = focusUiState.activeProfile?.name,
            timerState = timerState,
            onDismiss = {
                focusViewModel.stopFocusAndDeactivate()
            }
        )

        // App lock screen overlay
        if (isLocked && lockPin != null) {
            AppLockScreen(
                correctPin = lockPin!!,
                onUnlock = { isLocked = false }
            )
        }
    }

    } // end MONOSPACETheme
}
