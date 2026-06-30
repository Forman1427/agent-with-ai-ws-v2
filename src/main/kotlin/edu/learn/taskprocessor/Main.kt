package edu.learn.taskprocessor

import edu.learn.taskprocessor.config.AppConfig
import edu.learn.taskprocessor.database.DatabaseFactory
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import edu.learn.taskprocessor.plugins.configureCallLogging
import edu.learn.taskprocessor.plugins.configureCors
import edu.learn.taskprocessor.plugins.configureRouting
import edu.learn.taskprocessor.plugins.configureSerialization
import edu.learn.taskprocessor.plugins.configureWebSocket
import edu.learn.taskprocessor.repository.InMemoryTaskRepository
import edu.learn.taskprocessor.repository.PostgresTaskRepository
import edu.learn.taskprocessor.routes.UserWebSocketNotifier
import edu.learn.taskprocessor.routes.UserWebSocketSessionRegistry
import edu.learn.taskprocessor.services.MockAiServiceClient
import edu.learn.taskprocessor.services.RealAiServiceClient
import edu.learn.taskprocessor.services.TaskProcessingQueueImpl
import edu.learn.taskprocessor.services.TaskServiceImpl
import edu.learn.taskprocessor.services.UserProcessingServiceImpl
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val log = logger {}

fun main() {
    log.info { "Server starting on port 8080" }
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {


    val appConfig = AppConfig.instance.getConfig("processing")

    val useInMemoryRepository = AppConfig.booleanFromEnvOrConfig(
        "PROCESSING_USE_IN_MEMORY_REPOSITORY", appConfig, "useInMemoryRepository"
    )
    val taskRepository = if (useInMemoryRepository) InMemoryTaskRepository() else {
        DatabaseFactory.init()
        PostgresTaskRepository()
    }


    val useMockAi = AppConfig.booleanFromEnvOrConfig(
        "PROCESSING_USE_MOCK_AI", appConfig, "useMockAi"
    )
    val aiServiceClient = if (useMockAi) MockAiServiceClient() else RealAiServiceClient()

    val sessionRegistry = UserWebSocketSessionRegistry()
    val taskNotifier = UserWebSocketNotifier(sessionRegistry)
    val taskProcessingService = UserProcessingServiceImpl(taskRepository, aiServiceClient, taskNotifier)
    val processingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val workerCount = appConfig.getInt("workerCount")
    val taskProcessingQueue = TaskProcessingQueueImpl(
        taskProcessingService = taskProcessingService,
        workerCount = workerCount,
        scope = processingScope
    )
    val taskService = TaskServiceImpl(taskRepository, taskProcessingQueue)

    configureCors()
    configureSerialization()
    configureCallLogging()
    configureWebSocket()
    configureRouting(taskService, sessionRegistry)

    monitor.subscribe(ApplicationStopping) {
        log.info { "Shutting down..." }
        taskProcessingQueue.shutdown()
        DatabaseFactory.close()
    }
}