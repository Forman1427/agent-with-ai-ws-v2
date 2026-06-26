package edu.learn.taskprocessor.services

import edu.learn.taskprocessor.dto.WsServerMessage
import edu.learn.taskprocessor.model.Task
import edu.learn.taskprocessor.model.TaskStatus
import edu.learn.taskprocessor.repository.TaskRepository
import edu.learn.taskprocessor.routes.UserWebSocketNotifier
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

private val log = logger {}

class UserProcessingServiceImpl(
    private val repository: TaskRepository,
    private val aiServiceClient: AiServiceClient,
    private val notifier: UserWebSocketNotifier,
) : TaskProcessingService {

    override suspend fun processTask(taskId: UUID) {
        val task = repository.findById(taskId) ?: run {
            log.warn { "Task $taskId not found at start of processTask" }
            return
        }

        try {
            markTaskAsInProgress(task)
            notifier.notify(task.userName, buildStartedMessage(task))

            val result = coroutineScope {
                val progressJob = launch {
                    simulateProgress(taskId)
                }

                val r = withTimeout(30.seconds) {
                    aiServiceClient.generateText(task.taskText)
                }
                progressJob.cancelAndJoin()
                r
            }

            completeTask(taskId, result)

            val completedTask = repository.findById(taskId) ?: run {
                log.warn { "Task $taskId not found after completion" }
                return
            }
            notifier.notify(completedTask.userName, buildCompletedMessage(completedTask))
        } catch (e: Exception) {
            failTask(taskId, e)
        }
    }

    private fun markTaskAsInProgress(task: Task) {
        repository.update(task.copy(status = TaskStatus.IN_PROGRESS, progress = 0))
    }

    private suspend fun simulateProgress(taskId: UUID) {
        val steps = listOf(10, 20, 30, 40, 50, 60, 70, 80)

        for (step in steps) {
            delay(2_000L)

            val current = repository.findById(taskId) ?: run {
                log.warn { "Task $taskId not found during progress simulation" }
                return
            }
            if (current.status != TaskStatus.IN_PROGRESS) return

            repository.update(current.copy(progress = step))

            notifier.notify(current.userName, buildProgressMessage(current.copy(progress = step), "Прогресс: $step%"))
        }
    }

    private fun completeTask(taskId: UUID, result: String) {
        val task = repository.findById(taskId) ?: run {
            log.warn { "Task $taskId not found for completion" }
            return
        }
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        repository.update(task.copy(status = TaskStatus.COMPLETED, progress = 100, completedAt = now, resultText = result))
    }

    private suspend fun failTask(taskId: UUID, error: Exception) {
        val task = repository.findById(taskId) ?: run {
            log.warn { "Task $taskId not found for failure handling" }
            return
        }

        repository.update(task.copy(status = TaskStatus.FAILED))

        notifier.notify(task.userName, buildFailedMessage(task.copy(status = TaskStatus.FAILED), error))
    }

    private fun buildStartedMessage(task: Task): WsServerMessage =
        WsServerMessage(
            type = "TASK_STARTED",
            taskId = task.id.toString(),
            status = task.status.name,
            progress = task.progress,
            message = "Начали обработку задачи"
        )

    private fun buildProgressMessage(task: Task, progressMessage: String): WsServerMessage =
        WsServerMessage(
            type = "TASK_PROGRESS",
            taskId = task.id.toString(),
            status = task.status.name,
            progress = task.progress,
            message = progressMessage
        )

    private fun buildCompletedMessage(task: Task): WsServerMessage =
        WsServerMessage(
            type = "TASK_COMPLETED",
            taskId = task.id.toString(),
            status = task.status.name,
            progress = task.progress,
            message = "Задача выполнена",
            resultText = task.resultText,
            completedAt = task.completedAt?.toString()
        )

    private fun buildFailedMessage(task: Task, error: Exception): WsServerMessage =
        WsServerMessage(
            type = "TASK_FAILED",
            taskId = task.id.toString(),
            status = task.status.name,
            progress = task.progress,
            message = "Ошибка при обработке задачи",
            errorText = error.message
        )
}