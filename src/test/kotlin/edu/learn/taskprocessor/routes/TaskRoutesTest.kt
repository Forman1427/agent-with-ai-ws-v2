package edu.learn.taskprocessor.routes

import edu.learn.taskprocessor.dto.TaskResponse
import edu.learn.taskprocessor.plugins.configureRouting
import edu.learn.taskprocessor.plugins.configureSerialization
import edu.learn.taskprocessor.plugins.configureWebSocket
import edu.learn.taskprocessor.services.TaskService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import java.util.UUID
import kotlinx.serialization.json.Json

/**
 * Интеграционные тесты HTTP-роутов /api/v1/tasks с использованием testApplication.
 * Здесь мы тестируем только HTTP-слой (валидацию, коды ответов, JSON),
 * а TaskService мокируем.
 */
class TaskRoutesTest {

    /**
     * Общий модуль приложения для тестов:
     * подключаем те же плагины, что в боевом коде, но TaskService передаём мокнутый.
     */
    private fun Application.testModule(taskService: TaskService) {
        configureSerialization()
        configureWebSocket()
        configureRouting(taskService, UserWebSocketSessionRegistry())
    }

    /**
     * Общий клиент с ContentNegotiation + JSON для всех тестов,
     * чтобы можно было делать response.body<TaskResponse>() и response.body<List<TaskResponse>>().
     */
    private fun HttpClientConfig<*>.configureJsonClient() {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                }
            )
        }
    }

    @Test
    fun `POST api v1 tasks should create task`() = testApplication {
        val taskService = mockk<TaskService>()
        val taskId = UUID.randomUUID()

        every { taskService.createTask(any()) } returns TaskResponse(
            taskId = taskId.toString(),
            userName = "maksim",
            taskText = "Сделай краткое резюме",
            status = "QUEUED",
            progress = 0,
            createdAt = LocalDateTime(2026, 6, 25, 0, 0, 0, 0),
            completedAt = null,
            resultText = null
        )

        application {
            testModule(taskService)
        }

        val client = createClient { configureJsonClient() }

        val response = client.post("/api/v1/tasks") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "userName": "maksim",
                  "taskText": "Сделай краткое резюме"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.body<TaskResponse>()
        assertEquals(taskId.toString(), body.taskId)
        assertEquals("maksim", body.userName)
        assertEquals("Сделай краткое резюме", body.taskText)
        assertEquals("QUEUED", body.status)
        assertEquals(0, body.progress)
    }

    @Test
    fun `POST api v1 tasks should return bad request when userName blank`() = testApplication {
        val taskService = mockk<TaskService>(relaxed = true)

        application {
            testModule(taskService)
        }

        // Здесь нам не нужен JSON-клиент, достаточно дефолтного
        val response = client.post("/api/v1/tasks") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "userName": "   ",
                  "taskText": "Text"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.body<String>()
        assertTrue(body.contains("userName must not be blank"))
    }

    @Test
    fun `POST api v1 tasks should return bad request when taskText blank`() = testApplication {
        val taskService = mockk<TaskService>(relaxed = true)

        application {
            testModule(taskService)
        }

        val response = client.post("/api/v1/tasks") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "userName": "maksim",
                  "taskText": "   "
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.body<String>()
        assertTrue(body.contains("taskText must not be blank"))
    }

    @Test
    fun `GET api v1 tasks should return tasks by user`() = testApplication {
        val taskService = mockk<TaskService>()
        val taskId = UUID.randomUUID()

        every { taskService.getTasksByUser("maksim") } returns listOf(
            TaskResponse(
                taskId = taskId.toString(),
                userName = "maksim",
                taskText = "Задача 1",
                status = "COMPLETED",
                progress = 100,
                createdAt = LocalDateTime(2026, 6, 25, 0, 0, 0, 0),
                completedAt = LocalDateTime(2026, 6, 25, 0, 10, 0, 0),
                resultText = "Готово"
            )
        )

        application {
            testModule(taskService)
        }

        val client = createClient { configureJsonClient() }

        val response = client.get("/api/v1/tasks?userName=maksim")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.body<List<TaskResponse>>()
        assertEquals(1, body.size)
        val task = body.first()
        assertEquals("maksim", task.userName)
        assertEquals("Задача 1", task.taskText)
        assertEquals("COMPLETED", task.status)
        assertEquals("Готово", task.resultText)
    }

    @Test
    fun `GET api v1 tasks should return bad request when userName missing`() = testApplication {
        val taskService = mockk<TaskService>(relaxed = true)

        application {
            testModule(taskService)
        }

        val response = client.get("/api/v1/tasks")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.body<String>()
        assertTrue(body.contains("userName parameter is required"))
    }

    @Test
    fun `GET api v1 tasks by id should return task`() = testApplication {
        val taskService = mockk<TaskService>()
        val taskId = UUID.randomUUID()

        every { taskService.getTaskById(taskId) } returns TaskResponse(
            taskId = taskId.toString(),
            userName = "maksim",
            taskText = "Задача по id",
            status = "IN_PROGRESS",
            progress = 50,
            createdAt = LocalDateTime(2026, 6, 25, 0, 0, 0, 0),
            completedAt = null,
            resultText = null
        )

        application {
            testModule(taskService)
        }

        val client = createClient { configureJsonClient() }

        val response = client.get("/api/v1/tasks/$taskId")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.body<TaskResponse>()
        assertEquals(taskId.toString(), body.taskId)
        assertEquals("maksim", body.userName)
        assertEquals("Задача по id", body.taskText)
        assertEquals("IN_PROGRESS", body.status)
        assertEquals(50, body.progress)
    }

    @Test
    fun `GET api v1 tasks by id should return not found when service throws`() = testApplication {
        val taskService = mockk<TaskService>()

        every { taskService.getTaskById(any()) } throws NoSuchElementException("Task not found")

        application {
            testModule(taskService)
        }

        val response = client.get("/api/v1/tasks/${UUID.randomUUID()}")

        assertEquals(HttpStatusCode.NotFound, response.status)
        val body = response.body<String>()
        assertTrue(body.contains("Task not found"))
    }

    @Test
    fun `GET api v1 tasks by id should return bad request when id is invalid`() = testApplication {
        val taskService = mockk<TaskService>(relaxed = true)

        application {
            testModule(taskService)
        }

        val response = client.get("/api/v1/tasks/not-a-uuid")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.body<String>()
        assertTrue(body.contains("Invalid taskId format"))
    }
}