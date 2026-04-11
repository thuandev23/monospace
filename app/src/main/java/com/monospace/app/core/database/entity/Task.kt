package com.monospace.app.core.database.entity

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val isCompleted: Boolean = false
)