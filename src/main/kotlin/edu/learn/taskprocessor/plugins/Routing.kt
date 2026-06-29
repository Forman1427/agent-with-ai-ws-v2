package edu.learn.taskprocessor.plugins

import edu.learn.taskprocessor.routes.UserWebSocketSessionRegistry
import edu.learn.taskprocessor.routes.taskRoutes
import edu.learn.taskprocessor.routes.userWebSocketRoute
import edu.learn.taskprocessor.services.TaskService
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
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

        /** ===== СТАТИКА: index.html из resources/static =====
         staticResources мапит каталог "static" из classpath (src/main/resources/static)
         на URL-префикс "/static". Если внутри есть index.html, по /static он будет отдан.
         */
        staticResources("/static", "static", index = "index.html")
    }
}