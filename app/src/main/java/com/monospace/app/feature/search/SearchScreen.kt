package com.monospace.app.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.R
import com.monospace.app.core.domain.model.Task
import com.monospace.app.feature.launcher.components.TaskItem
import com.monospace.app.ui.theme.FocusTheme

@Composable
fun SearchScreen(
    onNavigateToTask: (taskId: String) -> Unit = {},
    onNavigateToCreateTask: () -> Unit = {},
    onClose: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var visible by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateTask,
                containerColor = FocusTheme.colors.primary,
                contentColor = FocusTheme.colors.background,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_add_task), modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Search bar + X button
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    initialOffsetY = { -it }
                ) + fadeIn(tween(200))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused },
                        placeholder = {
                            Text(
                                stringResource(R.string.label_search),
                                style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary)
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = FocusTheme.colors.secondary, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (uiState.query.isNotBlank()) {
                                IconButton(onClick = viewModel::clearQuery) {
                                    Icon(Icons.Default.Close, null, tint = FocusTheme.colors.secondary, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FocusTheme.colors.primary,
                            unfocusedBorderColor = FocusTheme.colors.divider,
                            focusedTextColor = FocusTheme.colors.primary,
                            unfocusedTextColor = FocusTheme.colors.primary
                        )
                    )
                    AnimatedVisibility(
                        visible = isFocused,
                        enter = fadeIn(tween(150)) + slideInHorizontally { it },
                        exit = fadeOut(tween(150)) + slideOutHorizontally { it }
                    ) {
                        IconButton(
                            onClick = { focusManager.clearFocus() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = FocusTheme.colors.secondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            } // end AnimatedVisibility (search bar)

            Spacer(Modifier.height(16.dp))

            when {
                !uiState.isSearching -> {
                    // Empty state before typing
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(300, delayMillis = 150)) + scaleIn(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            initialScale = 0.85f
                        )
                    ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = FocusTheme.colors.divider,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                stringResource(R.string.label_search_hint),
                                style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.secondary)
                            )
                            Text(
                                "Tap the input box to search",
                                style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary.copy(alpha = 0.6f))
                            )
                        }
                    }
                    } // end AnimatedVisibility (empty state)
                }

                uiState.results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No results for \"${uiState.query}\"",
                            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary)
                        )
                    }
                }

                else -> {
                    SearchResultList(
                        tasks = uiState.results,
                        onTaskClick = onNavigateToTask
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultList(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(items = tasks, key = { it.id }) { task ->
            TaskItem(
                task = task,
                isSelected = false,
                isSelectionMode = false,
                onToggle = { _ -> },
                onClick = { onTaskClick(task.id) },
                onLongClick = {}
            )
            HorizontalDivider(
                color = FocusTheme.colors.divider.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
        }
    }
}
