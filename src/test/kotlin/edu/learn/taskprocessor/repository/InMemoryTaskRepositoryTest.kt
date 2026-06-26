package edu.learn.taskprocessor.repository

import edu.learn.taskprocessor.model.Task
import edu.learn.taskprocessor.model.TaskStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import java.util.UUID
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class InMemoryTaskRepositoryTest {

    private val repository = InMemoryTaskRepository()

    private val fixedNow = Instant.parse("2026-06-24T15:00:00Z")
        .toLocalDateTime(TimeZone.UTC)

    @Test
    fun `save should store and return task`() {
        val task = createTask(
            id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            userName = "maxim",
            taskText = "Task 1"
        )

        val saved = repository.save(task)

        assertEquals(task, saved)
        assertEquals(task, repository.findById(task.id))
    }

    @Test
    fun `update should replace existing task`() {
        val task = createTask(
            id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
            userName = "maxim",
            taskText = "Original text"
        )
        repository.save(task)

        val updated = task.copy(
            status = TaskStatus.IN_PROGRESS,
            progress = 50
        )

        val result = repository.update(updated)

        assertEquals(updated, result)
        assertEquals(updated, repository.findById(task.id))
    }

    @Test
    fun `update should throw when task does not exist`() {
        val task = createTask(
            id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
            userName = "maxim",
            taskText = "Missing task"
        )

        val exception = assertFailsWith<NoSuchElementException> {
            repository.update(task)
        }

        assertEquals("Task with id=${task.id} not found", exception.message)
    }

    @Test
    fun `findById should return null when task does not exist`() {
        val result = repository.findById(
            UUID.fromString("44444444-4444-4444-4444-444444444444")
        )

        assertNull(result)
    }

    @Test
    fun `findByUserName should return tasks sorted by createdAt`() {
        val first = createTask(
            id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
            userName = "maxim",
            taskText = "First task",
            createdAt = Instant.parse("2026-06-24T10:00:00Z").toLocalDateTime(TimeZone.UTC)
        )

        val second = createTask(
            id = UUID.fromString("66666666-6666-6666-6666-666666666666"),
            userName = "maxim",
            taskText = "Second task",
            createdAt = Instant.parse("2026-06-24T11:00:00Z").toLocalDateTime(TimeZone.UTC)
        )

        val otherUserTask = createTask(
            id = UUID.fromString("77777777-7777-7777-7777-777777777777"),
            userName = "other",
            taskText = "Other user's task",
            createdAt = Instant.parse("2026-06-24T09:00:00Z").toLocalDateTime(TimeZone.UTC)
        )

        repository.save(second)
        repository.save(otherUserTask)
        repository.save(first)

        val result = repository.findByUserName("maxim")

        assertEquals(2, result.size)
        assertEquals(first, result[0])
        assertEquals(second, result[1])
    }

    @Test
    fun `findByUserName should return empty list when user has no tasks`() {
        val result = repository.findByUserName("unknown")

        assertEquals(emptyList<Task>(), result)
    }

    private fun createTask(
        id: UUID = UUID.randomUUID(),
        userName: String,
        taskText: String,
        createdAt: kotlinx.datetime.LocalDateTime = fixedNow
    ): Task {
        return Task(
            id = id,
            userName = userName,
            taskText = taskText,
            status = TaskStatus.QUEUED,
            progress = 0,
            createdAt = createdAt
        )
    }
}