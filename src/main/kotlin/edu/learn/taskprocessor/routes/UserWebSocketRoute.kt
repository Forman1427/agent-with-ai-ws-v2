package edu.learn.taskprocessor.routes

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val log = logger {}

fun Route.userWebSocketRoute(
    sessionRegistry: UserWebSocketSessionRegistry
) {
    webSocket("/ws/tasks") {
        val userName = call.request.queryParameters["userName"]
        if (userName.isNullOrBlank()) {
            close(
                CloseReason(
                    CloseReason.Codes.CANNOT_ACCEPT,
                    "userName is required"
                )
            )
            return@webSocket
        }

        log.info { "WebSocket connected: user=$userName" }
        sessionRegistry.register(userName, this)

        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()

                    if (text.equals("ping", ignoreCase = true)) {
                        send(Frame.Text(Json.encodeToString(buildJsonObject { put("type", "PONG") })))
                    }
                }
            }
        } finally {
            sessionRegistry.unregister(userName, this)
            log.info { "WebSocket disconnected: user=$userName" }
        }
    }
}