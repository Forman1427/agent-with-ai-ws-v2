package edu.learn.taskprocessor.plugins

import edu.learn.taskprocessor.routes.UserWebSocketSessionRegistry
import edu.learn.taskprocessor.routes.taskRoutes
import edu.learn.taskprocessor.routes.userWebSocketRoute
import edu.learn.taskprocessor.services.TaskService
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(
    taskService: TaskService,
    sessionRegistry: UserWebSocketSessionRegistry
) {
    routing {

        get("/") {
            call.respondText("OK from root")
        }
        taskRoutes(taskService)
        userWebSocketRoute(sessionRegistry)
    }
}