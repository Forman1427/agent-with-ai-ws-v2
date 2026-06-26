package edu.learn.taskprocessor.services

import java.util.UUID

interface TaskProcessingQueue {
    fun submit(taskId: UUID)
    fun shutdown()
}