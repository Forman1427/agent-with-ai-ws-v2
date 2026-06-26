package edu.learn.taskprocessor.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTaskRequest(
    val userName: String,
    val taskText: String
)