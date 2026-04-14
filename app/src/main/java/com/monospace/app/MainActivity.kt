package com.monospace.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.monospace.app.feature.launcher.components.HomeBottomBar
import com.monospace.app.ui.navigation.MonospaceNavGraph
import com.monospace.app.ui.navigation.Screen
import com.monospace.app.ui.theme.FocusTheme
import com.monospace.app.ui.theme.MONOSPACETheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MONOSPACETheme {
                MainScreen()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
    val isOnline by connectivityViewModel.isOnline.collectAsState()

    // Request POST_NOTIFICATIONS permission trên Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* granted hay không đều ok — reminder vẫn work, chỉ notification bị tắt */ }
        LaunchedEffect(Unit) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Upcoming.route,
        Screen.Tasks.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier.padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HomeBottomBar(
                        onTodayClick = {
                            navController.navigate(Screen.Home.BASE) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onSearchClick = {
                            navController.navigate(Screen.Home.withSearch()) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = false
                                }
                                launchSingleTop = false
                            }
                        },
                        onUpcomingClick = {
                            navController.navigate(Screen.Upcoming.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
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
            // Offline banner — hiện ở trên cùng khi mất mạng
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
}
