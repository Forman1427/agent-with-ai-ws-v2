package edu.learn.taskprocessor.dto

import kotlinx.serialization.Serializable

@Serializable
data class WsServerMessage(
    val type: String,
    val taskId: String,
    val status: String,
    val progress: Int,
    val message: String? = null,
    val resultText: String? = null,
    val errorText: String? = null,
    val completedAt: String? = null
)