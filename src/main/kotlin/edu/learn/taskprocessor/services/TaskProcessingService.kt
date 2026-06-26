package edu.learn.taskprocessor.services

import java.util.UUID

interface TaskProcessingService {
    suspend fun processTask(taskId: UUID)
}