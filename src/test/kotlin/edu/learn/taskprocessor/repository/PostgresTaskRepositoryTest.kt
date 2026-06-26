package edu.learn.taskprocessor.repository

import edu.learn.taskprocessor.model.Task
import edu.learn.taskprocessor.model.TaskStatus
import edu.learn.taskprocessor.repository.tables.TasksTable
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresTaskRepositoryTest {

    private val repository = PostgresTaskRepository()

    private val fixedNow = Instant.parse("2026-06-24T15:00:00Z")
            .toLocalDateTime(TimeZone.UTC)

    @BeforeAll
    fun setupDatabase() {
        Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver"
        )
        transaction {
            SchemaUtils.create(TasksTable)
        }
    }

    @AfterAll
    fun teardownDatabase() {
        transaction {
            SchemaUtils.drop(TasksTable)
        }
    }

    @AfterEach
    fun cleanup() {
        transaction {
            TasksTable.deleteAll()
        }
    }

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
            userName = "maxim", taskText = "Original text"
        )
        repository.save(task)
        val updated = task.copy(status = TaskStatus.IN_PROGRESS, progress = 50)
        val result = repository.update(updated)
        assertEquals(updated, result)
        assertEquals(updated, repository.findById(task.id))
    }

    @Test
    fun `update should throw when task does not exist`() {
        val task = createTask(
            id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
            userName = "maxim", taskText = "Missing task"
        )
        assertFailsWith<NoSuchElementException> { repository.update(task) }
    }

    @Test
    fun `findById should return task when exists`() {
        val task = createTask(
            id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
            userName = "maxim", taskText = "Find me"
        )
        repository.save(task)
        val found = repository.findById(task.id)
        assertNotNull(found)
        assertEquals(task, found)
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
            userName = "maxim", taskText = "First task",
            createdAt = Instant.parse("2026-06-24T10:00:00Z").toLocalDateTime(TimeZone.UTC)
        )
        val second = createTask(
            id = UUID.fromString("66666666-6666-6666-6666-666666666666"),
            userName = "maxim", taskText = "Second task",
            createdAt = Instant.parse("2026-06-24T11:00:00Z").toLocalDateTime(TimeZone.UTC)
        )
        val otherUserTask = createTask(
            id = UUID.fromString("77777777-7777-7777-7777-777777777777"),
            userName = "other", taskText = "Other user's task",
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
        assertEquals(emptyList<Task>(), repository.findByUserName("unknown"))
    }

    @Test
    fun `findByUserName should return tasks only for the specified user`() {
        val maximTask = createTask(
            id = UUID.fromString("88888888-8888-8888-8888-888888888888"),
            userName = "maxim", taskText = "Maxim's task"
        )
        val otherTask = createTask(
            id = UUID.fromString("99999999-9999-9999-9999-999999999999"),
            userName = "other", taskText = "Other's task"
        )
        repository.save(maximTask)
        repository.save(otherTask)
        val result = repository.findByUserName("maxim")
        assertEquals(1, result.size)
        assertEquals(maximTask, result[0])
    }

    @Test
    fun `save and update should handle nullable fields correctly`() {
        val task = createTask(
            id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
            userName = "maxim", taskText = "Nullable fields test"
        )
        repository.save(task)

        val saved = repository.findById(task.id)
        assertNotNull(saved)
        assertNull(saved.completedAt)
        assertNull(saved.resultText)

        val completedNow = Instant.parse("2026-06-24T16:00:00Z")
            .toLocalDateTime(TimeZone.UTC)
        val completed = task.copy(
            status = TaskStatus.COMPLETED, progress = 100,
            completedAt = completedNow, resultText = "Result text"
        )
        repository.update(completed)

        val updated = repository.findById(task.id)
        assertNotNull(updated)
        assertEquals(TaskStatus.COMPLETED, updated.status)
        assertEquals(100, updated.progress)
        assertEquals(completedNow, updated.completedAt)
        assertEquals("Result text", updated.resultText)
    }

    private fun createTask(
        id: UUID = UUID.randomUUID(),
        userName: String,
        taskText: String,
        createdAt: kotlinx.datetime.LocalDateTime = fixedNow
    ): Task = Task(
        id = id, userName = userName, taskText = taskText,
        status = TaskStatus.QUEUED, progress = 0, createdAt = createdAt
    )
}