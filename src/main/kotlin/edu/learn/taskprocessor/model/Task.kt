package edu.learn.taskprocessor.model

import kotlinx.datetime.LocalDateTime
import java.util.UUID

data class Task(
    val id: UUID,
    val userName: String,
    val taskText: String,
    val status: TaskStatus,
    val progress: Int,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime? = null,
    val resultText: String? = null
)
