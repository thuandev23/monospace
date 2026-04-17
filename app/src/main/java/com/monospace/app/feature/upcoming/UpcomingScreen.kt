package com.monospace.app.feature.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.R
import com.monospace.app.feature.launcher.components.SwipeableTaskItem
import com.monospace.app.ui.theme.FocusTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UpcomingScreen(
    onNavigateToTask: (taskId: String) -> Unit = {},
    viewModel: UpcomingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = FocusTheme.colors.primary,
                    contentColor = FocusTheme.colors.background
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.label_upcoming),
                        style = FocusTheme.typography.title.copy(
                            color = FocusTheme.colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                actions = {
                    if (uiState is UpcomingUiState.Success) {
                        val state = uiState as UpcomingUiState.Success
                        IconButton(onClick = viewModel::toggleShowCompleted) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = stringResource(
                                    if (state.showCompleted) R.string.action_hide_completed
                                    else R.string.action_show_completed
                                ),
                                tint = if (state.showCompleted) FocusTheme.colors.primary
                                       else FocusTheme.colors.secondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FocusTheme.colors.background
                )
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is UpcomingUiState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FocusTheme.colors.primary)
                }
            }

            is UpcomingUiState.Success -> {
                if (state.groups.isEmpty() && state.completedTasks.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.msg_no_upcoming_tasks),
                                style = FocusTheme.typography.title.copy(
                                    color = FocusTheme.colors.secondary
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(
                            start = 24.dp,
                            end = 24.dp,
                            top = 0.dp,
                            bottom = 100.dp
                        )
                    ) {
                        state.groups.forEach { group ->
                            stickyHeader(key = group.type.name) {
                                SectionHeader(label = group.type.label())
                            }
                            items(items = group.tasks, key = { it.id }) { task ->
                                SwipeableTaskItem(
                                    task = task,
                                    isSelected = false,
                                    isSelectionMode = false,
                                    onToggle = { completed -> viewModel.toggleTask(task.id, completed) },
                                    onClick = { onNavigateToTask(task.id) },
                                    onLongClick = {},
                                    onSwipeComplete = { viewModel.toggleTask(task.id, true) },
                                    onSwipeDelete = { viewModel.deleteTask(task.id) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }

                        if (state.showCompleted && state.completedTasks.isNotEmpty()) {
                            stickyHeader(key = "completed_header") {
                                SectionHeader(
                                    label = stringResource(R.string.label_completed_count, state.completedTasks.size)
                                )
                            }
                            items(items = state.completedTasks, key = { "done_${it.id}" }) { task ->
                                SwipeableTaskItem(
                                    task = task,
                                    isSelected = false,
                                    isSelectionMode = false,
                                    onToggle = { completed -> viewModel.toggleTask(task.id, completed) },
                                    onClick = { onNavigateToTask(task.id) },
                                    onLongClick = {},
                                    onSwipeComplete = { viewModel.toggleTask(task.id, false) },
                                    onSwipeDelete = { viewModel.deleteTask(task.id) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingGroupType.label(): String = stringResource(
    when (this) {
        UpcomingGroupType.OVERDUE -> R.string.label_group_overdue
        UpcomingGroupType.TODAY -> R.string.label_group_today
        UpcomingGroupType.TOMORROW -> R.string.label_group_tomorrow
        UpcomingGroupType.THIS_WEEK -> R.string.label_group_this_week
        UpcomingGroupType.LATER -> R.string.label_group_later
        UpcomingGroupType.NO_DATE -> R.string.label_group_no_date_scheduled
    }
)

@Composable
private fun SectionHeader(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FocusTheme.colors.background)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = FocusTheme.typography.label.copy(
                color = FocusTheme.colors.secondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        )
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = FocusTheme.colors.divider
        )
    }
}
