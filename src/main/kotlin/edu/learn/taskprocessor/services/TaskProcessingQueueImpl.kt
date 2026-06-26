package edu.learn.taskprocessor.services

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

private val log = logger {}

class TaskProcessingQueueImpl(
    private val taskProcessingService: TaskProcessingService,
    workerCount: Int = 2,
//    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scope: CoroutineScope
) : TaskProcessingQueue {

    private val queue = Channel<UUID>(capacity = 100)
    private val workers = mutableListOf<Job>()

    init {
        repeat(workerCount) {
            workers.add(scope.launch {
                try {
                    for (taskId in queue) {
                        taskProcessingService.processTask(taskId)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.error(e) { "Worker crashed, restarting..." }
                }
            })
        }
    }

    override fun submit(taskId: UUID) {
        val result = queue.trySend(taskId)
        when {
            result.isSuccess -> log.info { "Task $taskId submitted to queue" }
            result.isClosed -> log.warn { "Failed to submit task $taskId — queue is closed" }
            else -> log.warn { "Failed to submit task $taskId — queue is full" }
        }
    }

    override fun shutdown() {
        queue.close()
        workers.forEach { it.cancel() }
        runBlocking {
            workers.joinAll()
        }
        log.info { "All workers finished" }
    }
}