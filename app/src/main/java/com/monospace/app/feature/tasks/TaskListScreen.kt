package com.monospace.app.feature.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.monospace.app.ui.theme.FocusTheme

@Composable
fun TaskListScreen() {
    Scaffold(
        containerColor = FocusTheme.colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Task Lists Screen",
                style = FocusTheme.typography.title,
                color = FocusTheme.colors.primary
            )
        }
    }
}
