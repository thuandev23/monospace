package com.monospace.app

//import com.monospace.app.feature.launcher.HomeScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.feature.launcher.state.LauncherViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val viewModel: LauncherViewModel = hiltViewModel()
                val tasks by viewModel.uiState.collectAsState()
                var taskTitle by remember { mutableStateOf("") }
                val apps by viewModel.filteredApps.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White) // Nền trắng tinh
                        .padding(horizontal = 32.dp, vertical = 64.dp)
                ) {


                    // 2. Search Field (Minimalist)
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraLight,
                            color = Color.Black
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "type to search...",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.ExtraLight
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Apps List (Filtered)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(apps) { app ->
                            Text(
                                text = app.name.lowercase(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.launchApp(app.packageName) }
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    // Ô nhập task mới
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        TextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add a new task...") }
                        )
                        Button(
                            onClick = {
                                viewModel.addTask(taskTitle)
                                taskTitle = "" // Xóa text sau khi thêm
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Add")
                        }
                    }

                    if (tasks.isEmpty()) {
                        Text(text = "No tasks. Take a breath.")
                    } else {
                        LazyColumn {
                            items(tasks) { task ->
                                Text(
                                    text = if (task.isCompleted) "× ${task.title}" else "○ ${task.title}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = if (task.isCompleted) Color.Gray else Color.Black,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                viewModel.toggleTask(
                                                    task.id,
                                                    task.isCompleted
                                                )
                                            },
                                            onLongClick = { viewModel.deleteTask(task.id) }
                                        )
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}