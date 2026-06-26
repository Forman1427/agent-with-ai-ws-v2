package edu.learn.taskprocessor.services

import edu.learn.taskprocessor.dto.CreateTaskRequest
import edu.learn.taskprocessor.dto.TaskResponse
import java.util.UUID

interface TaskService {
    fun createTask(request: CreateTaskRequest): TaskResponse
    fun getTasksByUser(userName: String): List<TaskResponse>
    fun getTaskById(id: UUID): TaskResponse
}