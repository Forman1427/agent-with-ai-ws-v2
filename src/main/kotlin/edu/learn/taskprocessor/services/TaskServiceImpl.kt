package edu.learn.taskprocessor.services

import edu.learn.taskprocessor.dto.CreateTaskRequest
import edu.learn.taskprocessor.dto.TaskResponse
import edu.learn.taskprocessor.model.Task
import edu.learn.taskprocessor.model.TaskStatus
import edu.learn.taskprocessor.repository.TaskRepository
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

private val log = logger {}

class TaskServiceImpl(
    private val repository: TaskRepository,
    private val taskProcessingQueue: TaskProcessingQueue
) : TaskService {

    override fun createTask(request: CreateTaskRequest): TaskResponse {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val task = Task(
            id = UUID.randomUUID(),
            userName = request.userName,
            taskText = request.taskText,
            status = TaskStatus.QUEUED,
            progress = 0,
            createdAt = now
        )

        repository.save(task)
        taskProcessingQueue.submit(task.id)
        log.info { "Task created: id=${task.id}, user=${task.userName}" }

        return task.toResponse()
    }

    override fun getTasksByUser(userName: String): List<TaskResponse> {
        return repository.findByUserName(userName).map { it.toResponse() }
    }

    override fun getTaskById(id: UUID): TaskResponse {
        val task = repository.findById(id) ?: throw NoSuchElementException("Task not found")
        return task.toResponse()
    }

    private fun Task.toResponse(): TaskResponse {
        return TaskResponse(
            taskId = id.toString(),
            userName = userName,
            taskText = taskText,
            status = status.name,
            progress = progress,
            createdAt = createdAt,
            completedAt = completedAt,
            resultText = resultText
        )
    }
}