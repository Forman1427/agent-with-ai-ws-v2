package edu.learn.taskprocessor.services

import edu.learn.taskprocessor.dto.WsServerMessage
import edu.learn.taskprocessor.model.Task
import edu.learn.taskprocessor.model.TaskStatus
import edu.learn.taskprocessor.repository.InMemoryTaskRepository
import edu.learn.taskprocessor.routes.UserWebSocketNotifier
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.util.UUID

class UserProcessingServiceImplTest {

    private lateinit var repository: InMemoryTaskRepository
    private lateinit var notifier: UserWebSocketNotifier
    private lateinit var aiServiceClient: AiServiceClient
    private lateinit var service: UserProcessingServiceImpl

    @BeforeEach
    fun setUp() {
        repository = InMemoryTaskRepository()
        notifier = mockk(relaxed = true)
        aiServiceClient = mockk()

        service = UserProcessingServiceImpl(
            repository = repository,
            aiServiceClient = aiServiceClient,
            notifier = notifier
        )
    }


    @Test
    fun `processTask should complete task successfully and send ws notifications`() = runTest {
        val taskId = UUID.randomUUID()
        val createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        repository.save(
            Task(
                id = taskId,
                userName = "maksim",
                taskText = "Напиши краткое резюме текста",
                status = TaskStatus.QUEUED,
                progress = 0,
                createdAt = createdAt
            )
        )

        coEvery { aiServiceClient.generateText(any()) }coAnswers {
            delay(20_000) // даём время simulateProgress отработать 8 шагов * 2с = 16с
            "готовый результат"
        }

        val capturedMessages = mutableListOf<WsServerMessage>()
        coEvery { notifier.notify(any(), capture(capturedMessages)) } returns Unit

        service.processTask(taskId)
        advanceUntilIdle()

        val updated = repository.findById(taskId)
        assertNotNull(updated)
        assertEquals(TaskStatus.COMPLETED, updated.status)
        assertEquals(100, updated.progress)
        assertEquals("готовый результат", updated.resultText)
        assertNotNull(updated.completedAt)

        val started = capturedMessages.firstOrNull { it.type == "TASK_STARTED" }
        assertNotNull(started)
        assertEquals(taskId.toString(), started.taskId)
        assertEquals("QUEUED", started.status)
        assertEquals(0, started.progress)
        assertEquals("Начали обработку задачи", started.message)

        val completed = capturedMessages.firstOrNull { it.type == "TASK_COMPLETED" }
        assertNotNull(completed)
        assertEquals(taskId.toString(), completed.taskId)
        assertEquals("COMPLETED", completed.status)
        assertEquals(100, completed.progress)
        assertEquals("Задача выполнена", completed.message)
        assertEquals("готовый результат", completed.resultText)
        assertNotNull(completed.completedAt)

        assertTrue(capturedMessages.any { it.type == "TASK_PROGRESS" })

        coVerify(exactly = 1) { aiServiceClient.generateText("Напиши краткое резюме текста") }
        coVerify(atLeast = 1) { notifier.notify("maksim", any()) }
    }

    @Test
    fun `processTask should mark task as failed when ai throws exception`() = runTest {
        val taskId = UUID.randomUUID()
        val createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        repository.save(
            Task(
                id = taskId,
                userName = "maksim",
                taskText = "Сгенерируй ответ",
                status = TaskStatus.QUEUED,
                progress = 0,
                createdAt = createdAt
            )
        )

        coEvery { aiServiceClient.generateText(any()) } throws RuntimeException("AI is down")

        val capturedMessages = mutableListOf<WsServerMessage>()
        coEvery { notifier.notify(any(), capture(capturedMessages)) } returns Unit

        service.processTask(taskId)
        advanceUntilIdle()

        val updated = repository.findById(taskId)
        assertNotNull(updated)
        assertEquals(TaskStatus.FAILED, updated.status)

        val failed = capturedMessages.firstOrNull { it.type == "TASK_FAILED" }
        assertNotNull(failed)
        assertEquals(taskId.toString(), failed.taskId)
        assertEquals("FAILED", failed.status)
        assertEquals("Ошибка при обработке задачи", failed.message)
        assertEquals("AI is down", failed.errorText)

        coVerify(exactly = 1) { aiServiceClient.generateText("Сгенерируй ответ") }
        coVerify(atLeast = 1) { notifier.notify("maksim", any()) }
    }

    @Test
    fun `processTask should do nothing when task not found`() = runTest {
        val taskId = UUID.randomUUID()

        service.processTask(taskId)
        advanceUntilIdle()

        coVerify(exactly = 0) { aiServiceClient.generateText(any()) }
        coVerify(exactly = 0) { notifier.notify(any(), any()) }
        assertNull(repository.findById(taskId))
    }
}