package edu.learn.taskprocessor.routes

import edu.learn.taskprocessor.dto.CreateTaskRequest
import edu.learn.taskprocessor.services.TaskService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.taskRoutes(taskService: TaskService) {
    route("/api/v1/tasks") {
        post {
            val request: CreateTaskRequest = call.receive()

            // Далее пара проверок, что отправляемые поля не пустые - мы же доверяем фронту, но проверяем
            if (request.userName.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "userName must not be blank"
                )
                return@post
            }

            if (request.taskText.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "taskText must not be blank"
                )
                return@post
            }

            val response = taskService.createTask(request)
            call.respond(HttpStatusCode.Created, response)
        }

        get {
            val userName = call.request.queryParameters["userName"]
            if (userName.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "userName parameter is required"
                )
                return@get
            }
            val tasks = taskService.getTasksByUser(userName)
            call.respond(HttpStatusCode.OK, tasks)
        }

        get("/{taskId}") {
            val taskIdParam = call.parameters["taskId"]

            if (taskIdParam.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "taskId parameter is required"
                )
                return@get
            }

            val taskId = try {
                UUID.fromString(taskIdParam)
            } catch (_: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Invalid taskId format"
                )
                return@get
            }

            try {
                val response = taskService.getTaskById(taskId)
                call.respond(HttpStatusCode.OK, response)
            } catch (_: NoSuchElementException) {
                call.respond(
                    HttpStatusCode.NotFound,
                    "Task not found"
                )
            }
        }
    }
}