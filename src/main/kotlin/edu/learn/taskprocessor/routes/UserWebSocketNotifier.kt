package edu.learn.taskprocessor.routes

import edu.learn.taskprocessor.dto.WsServerMessage
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.json.Json

private val log = logger {}

class UserWebSocketNotifier(
    private val sessionRegistry: UserWebSocketSessionRegistry,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {

    suspend fun notify(userName: String, message: WsServerMessage) {
        val payload = json.encodeToString(WsServerMessage.serializer(), message)
        val frame = Frame.Text(payload)

        val sessions: Set<WebSocketServerSession> = sessionRegistry.getSessions(userName)
        for (session in sessions) {
            try {
                session.send(frame)
            } catch (e: Exception) {
                log.error(e) { "Failed to send notification to user=$userName, unregistering session" }
                sessionRegistry.unregister(userName, session)
            }
        }
    }
}