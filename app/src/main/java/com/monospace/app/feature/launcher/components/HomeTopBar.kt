package com.monospace.app.feature.launcher.components

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.monospace.app.R
import com.monospace.app.ui.theme.FocusTheme
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onExitSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    isMenuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onSelectedTasks: () -> Unit
) {
    var showViewOptions by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onExitSelection) {
                        Text(
                            stringResource(R.string.action_cancel),
                            style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                        )
                    }
                    if (selectedCount > 0) {
                        Text(
                            "$selectedCount selected",
                            style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                if (!isSelectionMode) {
                    IconButton(
                        onClick = { onMenuToggle(true) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.content_desc_menu),
                            modifier = Modifier.size(28.dp),
                            tint = FocusTheme.colors.primary
                        )
                    }
                    MonospaceDropdownMenu(
                        expanded = isMenuExpanded,
                        onDismiss = { onMenuToggle(false) },
                        onSelectedTasks = onSelectedTasks,
                        onViewOptionsClick = {
                            onMenuToggle(false)
                            showViewOptions = true
                        }
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedCount > 0) {
                            IconButton(onClick = onDeleteSelected) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = FocusTheme.colors.destructive
                                )
                            }
                        }
                        TextButton(onClick = { /* Select All logic */ }) {
                            Text(
                                stringResource(R.string.action_select_all),
                                style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(R.string.label_today),
                style = FocusTheme.typography.displayLarge.copy(color = FocusTheme.colors.primary)
            )

            val calendar = Calendar.getInstance()
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = calendar.get(Calendar.DAY_OF_MONTH).toString(),
                    style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)
                )
                Text(
                    text = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH)
                        ?: "",
                    style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.secondary)
                )
            }
        }
    }

    if (showViewOptions) {
        ViewOptionsBottomSheet(onDismiss = { showViewOptions = false })
    }
}

@Composable
fun MonospaceDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelectedTasks: () -> Unit,
    onViewOptionsClick: () -> Unit
) {
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier
                .background(FocusTheme.colors.background)
                .width(200.dp)
                .border(0.5.dp, FocusTheme.colors.divider, RoundedCornerShape(20.dp))
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "View",
                        style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Menu,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = FocusTheme.colors.primary
                    )
                },
                onClick = onViewOptionsClick
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 0.5.dp,
                color = FocusTheme.colors.divider
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Select tasks",
                        style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.content_copy_24dp),
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = FocusTheme.colors.primary
                    )
                },
                onClick = onSelectedTasks
            )
        }
    }
}
