package com.monospace.app.core.data

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val isCompleted: Boolean = false
)
