package edu.learn.taskprocessor.routes

import edu.learn.taskprocessor.dto.WsServerMessage
import edu.learn.taskprocessor.plugins.configureRouting
import edu.learn.taskprocessor.plugins.configureWebSocket
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserWebSocketRouteTest {

    @Test
    fun `ws connection without userName should be rejected`() = testApplication {
        application {
            val registry = UserWebSocketSessionRegistry()
            configureWebSocket()
            configureRouting(
                taskService = FakeTaskService(),
                sessionRegistry = registry
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket(method = HttpMethod.Get, path = "/ws/tasks") {
            val reason: CloseReason? = closeReason.await()

            assertEquals(1003.toShort(), reason?.code)
            assertEquals("userName is required", reason?.message)
        }
    }

    @Test
    fun `ws connection should register session, answer ping and receive notify`() = testApplication {
        val registry = UserWebSocketSessionRegistry()
        val notifier = UserWebSocketNotifier(registry)

        application {
            configureWebSocket()
            configureRouting(
                taskService = FakeTaskService(),
                sessionRegistry = registry
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket(
            method = HttpMethod.Get,
            path = "/ws/tasks?userName=alex"
        ) {
            withTimeout(2_000) {
                while (registry.getSessions("alex").isEmpty()) {
                    kotlinx.coroutines.delay(10)
                }
            }

            assertEquals(1, registry.getSessions("alex").size)

            send(Frame.Text("ping"))

            val pong = withTimeout(2_000) {
                incoming.receive()
            }

            assertTrue(pong is Frame.Text)
            val pongText = (pong as Frame.Text).readText()
            assertTrue(pongText.contains("PONG"))

            notifier.notify(
                "alex",
                WsServerMessage(
                    type = "TASK_STARTED",
                    taskId = "task-1",
                    status = "IN_PROGRESS",
                    progress = 0,
                    message = "Started"
                )
            )

            val notification = withTimeout(2_000) {
                incoming.receive()
            }

            assertTrue(notification is Frame.Text)
            val notificationText = (notification as Frame.Text).readText()
            assertTrue(notificationText.contains("TASK_STARTED"))
            assertTrue(notificationText.contains("task-1"))
            assertTrue(notificationText.contains("Started"))
        }

        assertTrue(registry.getSessions("alex").isEmpty())
    }

    @Test
    fun `ws connection should unregister after client closes`() = testApplication {
        val registry = UserWebSocketSessionRegistry()

        application {
            configureWebSocket()
            configureRouting(
                taskService = FakeTaskService(),
                sessionRegistry = registry
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket(
            method = HttpMethod.Get,
            path = "/ws/tasks?userName=ivan"
        ) {
            assertEquals(1, registry.getSessions("ivan").size)
            close()
        }

        assertTrue(registry.getSessions("ivan").isEmpty())
    }
}

private class FakeTaskService : edu.learn.taskprocessor.services.TaskService {
    override fun createTask(request: edu.learn.taskprocessor.dto.CreateTaskRequest) =
        error("Not used in websocket tests")

    override fun getTasksByUser(userName: String) =
        error("Not used in websocket tests")

    override fun getTaskById(id: java.util.UUID) =
        error("Not used in websocket tests")
}