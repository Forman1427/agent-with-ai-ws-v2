package edu.learn.taskprocessor.routes

import edu.learn.taskprocessor.dto.WsServerMessage
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class UserWebSocketNotifierTest {

    private val sessionRegistry: UserWebSocketSessionRegistry = mockk()
    private val notifier = UserWebSocketNotifier(sessionRegistry)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `notify should send message to all sessions of user`() = runTest {
        val session1 = mockk<WebSocketServerSession>()
        val session2 = mockk<WebSocketServerSession>()

        val message = WsServerMessage(
            type = "TASK_STARTED",
            taskId = "task-1",
            status = "IN_PROGRESS",
            progress = 0,
            message = "Started"
        )

        every { sessionRegistry.getSessions("maxim") } returns setOf(session1, session2)
        coEvery { session1.send(any<Frame.Text>()) } just runs
        coEvery { session2.send(any<Frame.Text>()) } just runs

        notifier.notify("maxim", message)

        coVerify(exactly = 1) { session1.send(any<Frame.Text>()) }
        coVerify(exactly = 1) { session2.send(any<Frame.Text>()) }
        verify(exactly = 0) { sessionRegistry.unregister(any(), any()) }
    }

    @Test
    fun `notify should unregister session when sending fails`() = runTest {
        val goodSession = mockk<WebSocketServerSession>()
        val badSession = mockk<WebSocketServerSession>()

        val message = WsServerMessage(
            type = "TASK_PROGRESS",
            taskId = "task-2",
            status = "IN_PROGRESS",
            progress = 50,
            message = "Progress 50%"
        )

        every { sessionRegistry.getSessions("maxim") } returns setOf(goodSession, badSession)

        coEvery { goodSession.send(any<Frame.Text>()) } just runs
        coEvery { badSession.send(any<Frame.Text>()) } throws RuntimeException("socket closed")

        every { sessionRegistry.unregister("maxim", badSession) } just runs

        notifier.notify("maxim", message)

        coVerify(exactly = 1) { goodSession.send(any<Frame.Text>()) }
        coVerify(exactly = 1) { badSession.send(any<Frame.Text>()) }
        verify(exactly = 1) { sessionRegistry.unregister("maxim", badSession) }
        verify(exactly = 0) { sessionRegistry.unregister("maxim", goodSession) }
    }

    @Test
    fun `notify should serialize message to json`() = runTest {
        val session = mockk<WebSocketServerSession>()

        val message = WsServerMessage(
            type = "TASK_COMPLETED",
            taskId = "task-3",
            status = "COMPLETED",
            progress = 100,
            message = "Done",
            resultText = "Result text"
        )

        every { sessionRegistry.getSessions("maxim") } returns setOf(session)

        var capturedText: String? = null
        coEvery { session.send(any<Frame.Text>()) } coAnswers {
            val frame = firstArg<Frame.Text>()
            capturedText = frame.readText()
        }

        notifier.notify("maxim", message)

        assertTrue(capturedText != null)
        val expected = json.encodeToString(WsServerMessage.serializer(), message)
        assertTrue(capturedText == expected)
    }
}