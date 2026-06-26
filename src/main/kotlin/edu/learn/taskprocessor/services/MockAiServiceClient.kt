package edu.learn.taskprocessor.services

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import kotlinx.coroutines.delay

private val log = logger {}

class MockAiServiceClient(
    private val artificialDelayMs: Long = 20_000L
) : AiServiceClient {

    override suspend fun generateText(prompt: String): String {
        log.info { "Mock AI generation started, prompt length=${prompt.length}" }
        delay(artificialDelayMs)
        val result = "Сгенерированный MockAiServiceClient-ом MOCK-текст по запросу: \"$prompt\""
        log.info { "Mock AI generation completed" }
        return result
    }
}