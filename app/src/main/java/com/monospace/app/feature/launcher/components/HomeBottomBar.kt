package com.monospace.app.feature.launcher.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monospace.app.R
import com.monospace.app.ui.theme.FocusTheme
import java.util.Calendar

@Composable
fun HomeBottomBar(
    onTodayClick: () -> Unit,
    onUpcomingClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = FocusTheme.colors.surface,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(72.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Today (Icon with number)
            IconButton(onClick = onTodayClick) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(FocusTheme.colors.secondary.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val calendar = Calendar.getInstance()
                    Text(
                        text = calendar.get(Calendar.DAY_OF_MONTH).toString(),
                        style = FocusTheme.typography.label.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = FocusTheme.colors.primary
                        )
                    )
                }
            }

            // Tab 2: Upcoming (Calendar icon)
            IconButton(onClick = onUpcomingClick) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Upcoming",
                    tint = FocusTheme.colors.secondary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Tab 3: Search
            IconButton(onClick = onSearchClick) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = FocusTheme.colors.secondary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Tab 4: Settings
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = FocusTheme.colors.secondary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
