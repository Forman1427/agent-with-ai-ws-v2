package edu.learn.taskprocessor.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class TaskResponse(
    val taskId: String,
    val userName: String,
    val taskText: String,
    val status: String,
    val progress: Int,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val resultText: String?
)