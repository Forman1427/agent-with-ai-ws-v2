package edu.learn.taskprocessor.routes

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.ktor.server.websocket.WebSocketServerSession
import java.util.concurrent.ConcurrentHashMap

private val log = logger {}

class UserWebSocketSessionRegistry {

    private val sessionsByUserName: MutableMap<String, MutableSet<WebSocketServerSession>> =
        ConcurrentHashMap()

    fun register(userName: String, session: WebSocketServerSession) {
        val sessions = sessionsByUserName.computeIfAbsent(userName) {
            ConcurrentHashMap.newKeySet<WebSocketServerSession>()
        }
        sessions.add(session)
        log.info { "WebSocket registered: user=$userName" }
    }

    fun unregister(userName: String, session: WebSocketServerSession) {
        val sessions = sessionsByUserName[userName] ?: return
        sessions.remove(session)
        if (sessions.isEmpty()) {
            sessionsByUserName.remove(userName)
        }
        log.info { "WebSocket unregistered: user=$userName" }
    }

    fun getSessions(userName: String): Set<WebSocketServerSession> {
        return sessionsByUserName[userName]?.toSet() ?: emptySet()
    }

    fun getAllSessions(): Map<String, Set<WebSocketServerSession>> =
        sessionsByUserName.mapValues { (_, set) -> set.toSet() }
}