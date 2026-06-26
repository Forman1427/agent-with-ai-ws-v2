package edu.learn.taskprocessor.routes

import io.ktor.server.websocket.WebSocketServerSession
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserWebSocketSessionRegistryTest {

    private val registry = UserWebSocketSessionRegistry()

    @Test
    fun `register should add session for user`() {
        val session = mockk<WebSocketServerSession>(relaxed = true)

        registry.register("maxim", session)

        val sessions = registry.getSessions("maxim")
        assertEquals(1, sessions.size)
        assertTrue(session in sessions)
    }

    @Test
    fun `register should support multiple sessions for same user`() {
        val session1 = mockk<WebSocketServerSession>(relaxed = true)
        val session2 = mockk<WebSocketServerSession>(relaxed = true)

        registry.register("maxim", session1)
        registry.register("maxim", session2)

        val sessions = registry.getSessions("maxim")
        assertEquals(2, sessions.size)
        assertTrue(session1 in sessions)
        assertTrue(session2 in sessions)
    }

    @Test
    fun `unregister should remove session for user`() {
        val session1 = mockk<WebSocketServerSession>(relaxed = true)
        val session2 = mockk<WebSocketServerSession>(relaxed = true)

        registry.register("maxim", session1)
        registry.register("maxim", session2)

        registry.unregister("maxim", session1)

        val sessions = registry.getSessions("maxim")
        assertEquals(1, sessions.size)
        assertTrue(session2 in sessions)
    }

    @Test
    fun `unregister should remove user entry when last session removed`() {
        val session = mockk<WebSocketServerSession>(relaxed = true)

        registry.register("maxim", session)
        registry.unregister("maxim", session)

        assertTrue(registry.getSessions("maxim").isEmpty())
        assertTrue("maxim" !in registry.getAllSessions())
    }

    @Test
    fun `getSessions should return empty set for unknown user`() {
        val sessions = registry.getSessions("unknown")

        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `getAllSessions should return snapshot of all users`() {
        val session1 = mockk<WebSocketServerSession>(relaxed = true)
        val session2 = mockk<WebSocketServerSession>(relaxed = true)
        val session3 = mockk<WebSocketServerSession>(relaxed = true)

        registry.register("maxim", session1)
        registry.register("maxim", session2)
        registry.register("alex", session3)

        val allSessions = registry.getAllSessions()

        assertEquals(2, allSessions.size)
        assertEquals(2, allSessions["maxim"]?.size)
        assertEquals(1, allSessions["alex"]?.size)
    }
}