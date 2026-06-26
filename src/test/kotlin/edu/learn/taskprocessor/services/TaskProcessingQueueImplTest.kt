package edu.learn.taskprocessor.services

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class TaskProcessingQueueImplTest {

    private lateinit var scope: CoroutineScope
    private lateinit var taskProcessingService: TaskProcessingService
    private var queue: TaskProcessingQueueImpl? = null

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        taskProcessingService = mockk()
    }

    @AfterEach
    fun tearDown() {
        queue?.shutdown()
        scope.cancel()
    }

    @Test
    fun `submit should send task to processing service`() = runBlocking {
        val processed = CompletableDeferred<Unit>()

        coEvery { taskProcessingService.processTask(any()) } answers {
            processed.complete(Unit)
        }

        queue = TaskProcessingQueueImpl(
            taskProcessingService = taskProcessingService,
            workerCount = 1,
            scope = scope
        )

        val taskId = UUID.randomUUID()
        queue!!.submit(taskId)

        // Ждём, пока обработка произойдёт
        processed.await()

        coVerify(exactly = 1) { taskProcessingService.processTask(taskId) }
    }

    @Test
    fun `should process several submitted tasks`() = runBlocking {
        val processedIds = mutableListOf<UUID>()

        coEvery { taskProcessingService.processTask(any()) } answers {
            processedIds.add(firstArg())
        }

        queue = TaskProcessingQueueImpl(
            taskProcessingService = taskProcessingService,
            workerCount = 1,
            scope = scope
        )

        val taskId1 = UUID.randomUUID()
        val taskId2 = UUID.randomUUID()
        val taskId3 = UUID.randomUUID()

        queue!!.submit(taskId1)
        queue!!.submit(taskId2)
        queue!!.submit(taskId3)

        withTimeout(2_000L) {
            while (processedIds.size < 3) {
                delay(10)
            }
        }

        coVerify(exactly = 1) { taskProcessingService.processTask(taskId1) }
        coVerify(exactly = 1) { taskProcessingService.processTask(taskId2) }
        coVerify(exactly = 1) { taskProcessingService.processTask(taskId3) }
    }

    @Test
    fun `shutdown should finish workers without hanging`() = runBlocking {
        coEvery { taskProcessingService.processTask(any()) } coAnswers {
            delay(50)
        }

        queue = TaskProcessingQueueImpl(
            taskProcessingService = taskProcessingService,
            workerCount = 1,
            scope = scope
        )

        val taskId = UUID.randomUUID()
        queue!!.submit(taskId)

        // Дадим воркеру начать работу
        delay(100)

        // shutdown не должен зависнуть
        withTimeout(2_000L) {
            queue!!.shutdown()
        }
    }

    @Test
    fun `submit after shutdown should not hang`() = runBlocking {
        queue = TaskProcessingQueueImpl(
            taskProcessingService = taskProcessingService,
            workerCount = 1,
            scope = scope
        )

        queue!!.shutdown()

        assertDoesNotThrow {
            queue!!.submit(UUID.randomUUID())
        }
    }

    @Test
    fun `two workers should process tasks`() = runBlocking {
        val processedIds = mutableListOf<UUID>()

        coEvery { taskProcessingService.processTask(any()) }  coAnswers {
            processedIds.add(firstArg())
            delay(100)
        }

        queue = TaskProcessingQueueImpl(
            taskProcessingService = taskProcessingService,
            workerCount = 2,
            scope = scope
        )

        val taskId1 = UUID.randomUUID()
        val taskId2 = UUID.randomUUID()
        val taskId3 = UUID.randomUUID()
        val taskId4 = UUID.randomUUID()

        queue!!.submit(taskId1)
        queue!!.submit(taskId2)
        queue!!.submit(taskId3)
        queue!!.submit(taskId4)

        withTimeout(3_000L) {
            while (processedIds.size < 4) {
                delay(10)
            }
        }

        coVerify(exactly = 1) { taskProcessingService.processTask(taskId1) }
        coVerify(exactly = 1) { taskProcessingService.processTask(taskId2) }
        coVerify(exactly = 1) { taskProcessingService.processTask(taskId3) }
        coVerify(exactly = 1) { taskProcessingService.processTask(taskId4) }
    }
}