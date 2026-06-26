package edu.learn.taskprocessor.services

import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging.logger

private val log = logger {}

class RealAiServiceClient : AiServiceClient {
    private val dotenv = dotenv {
        directory = "."
        ignoreIfMalformed = true
        ignoreIfMissing = true
    }

    val apiKey: String = dotenv["AI_API_KEY"]
        ?: error("apiKey – не задан (переменная окружения API_KEY отсутствует)")

    override suspend fun generateText(prompt: String): String {
        log.info { "AI generation started, prompt length=${prompt.length}" }
        val result = callDeepSeekR1Free(apiKey, prompt)
        log.info { "AI generation completed" }
        return result
    }
}
