package edu.learn.taskprocessor.services

import edu.learn.taskprocessor.dto.CreateTaskRequest
import edu.learn.taskprocessor.model.Task
import edu.learn.taskprocessor.model.TaskStatus
import edu.learn.taskprocessor.repository.TaskRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class TaskServiceImplTest {

    private val repository: TaskRepository = mockk()
    private val taskProcessingQueue: TaskProcessingQueue = mockk()

    private val taskService = TaskServiceImpl(
        repository = repository,
        taskProcessingQueue = taskProcessingQueue
    )

    @Test
    fun `createTask should save task, submit to queue and return response`() {
        every { repository.save(any()) } answers { firstArg() }
        every { taskProcessingQueue.submit(any()) } just Runs

        val request = CreateTaskRequest(
            userName = "maxim",
            taskText = "Generate text"
        )

        val response = taskService.createTask(request)

        verify(exactly = 1) { repository.save(any()) }
        verify(exactly = 1) { taskProcessingQueue.submit(any()) }

        assertEquals("maxim", response.userName)
        assertEquals("Generate text", response.taskText)
        assertEquals("QUEUED", response.status)
        assertEquals(0, response.progress)
        assertEquals(null, response.completedAt)
        assertEquals(null, response.resultText)
        assertEquals(response.taskId, response.taskId)
    }

    @Test
    fun `getTasksByUser should return mapped task responses`() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val task1 = Task(
            id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            userName = "maxim",
            taskText = "First task",
            status = TaskStatus.QUEUED,
            progress = 0,
            createdAt = now
        )

        val task2 = Task(
            id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
            userName = "maxim",
            taskText = "Second task",
            status = TaskStatus.COMPLETED,
            progress = 100,
            createdAt = now,
            completedAt = now,
            resultText = "Done"
        )

        every { repository.findByUserName("maxim") } returns listOf(task1, task2)

        val responses = taskService.getTasksByUser("maxim")

        verify(exactly = 1) { repository.findByUserName("maxim") }

        assertEquals(2, responses.size)

        assertEquals("11111111-1111-1111-1111-111111111111", responses[0].taskId)
        assertEquals("maxim", responses[0].userName)
        assertEquals("First task", responses[0].taskText)
        assertEquals("QUEUED", responses[0].status)
        assertEquals(0, responses[0].progress)
        assertEquals(null, responses[0].completedAt)
        assertEquals(null, responses[0].resultText)

        assertEquals("22222222-2222-2222-2222-222222222222", responses[1].taskId)
        assertEquals("maxim", responses[1].userName)
        assertEquals("Second task", responses[1].taskText)
        assertEquals("COMPLETED", responses[1].status)
        assertEquals(100, responses[1].progress)
        assertEquals(now, responses[1].completedAt)
        assertEquals("Done", responses[1].resultText)
    }

    @Test
    fun `getTaskById should return task response when task exists`() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val taskId = UUID.fromString("33333333-3333-3333-3333-333333333333")

        val task = Task(
            id = taskId,
            userName = "maxim",
            taskText = "My task",
            status = TaskStatus.IN_PROGRESS,
            progress = 40,
            createdAt = now
        )

        every { repository.findById(taskId) } returns task

        val response = taskService.getTaskById(taskId)

        verify(exactly = 1) { repository.findById(taskId) }

        assertEquals(taskId.toString(), response.taskId)
        assertEquals("maxim", response.userName)
        assertEquals("My task", response.taskText)
        assertEquals("IN_PROGRESS", response.status)
        assertEquals(40, response.progress)
        assertEquals(now, response.createdAt)
        assertEquals(null, response.completedAt)
        assertEquals(null, response.resultText)
    }

    @Test
    fun `getTaskById should throw when task not found`() {
        val taskId = UUID.fromString("44444444-4444-4444-4444-444444444444")

        every { repository.findById(taskId) } returns null

        val exception = assertFailsWith<NoSuchElementException> {
            taskService.getTaskById(taskId)
        }

        assertEquals("Task not found", exception.message)

        verify(exactly = 1) { repository.findById(taskId) }
    }
}