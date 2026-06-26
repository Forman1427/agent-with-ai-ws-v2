package edu.learn.taskprocessor.services

import edu.learn.taskprocessor.dto.openrouter.ChatCompletionRequest
import edu.learn.taskprocessor.dto.openrouter.ChatCompletionResponse
import edu.learn.taskprocessor.dto.openrouter.ChatMessage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json

import kotlinx.serialization.json.Json

private val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }
        )
    }
}

suspend fun callDeepSeekR1Free(
    apiKey: String,
    prompt: String
): String {
    val requestBody = ChatCompletionRequest(
        model = "deepseek/deepseek-r1",
        messages = listOf(
            ChatMessage(
                role = "user",
                content = prompt
            )
        ),
        max_tokens = 512,
        temperature = 0.2
    )

    val response: HttpResponse = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer $apiKey")
        header("HTTP-Referer", "http://localhost:8080") // или твой реальный URL [web:29]
        header("X-Title", "Ktor DeepSeek Client")
        setBody(requestBody)
    }

    val responseBody = response.bodyAsText()

    if (!response.status.isSuccess()) {
        // тут можно попробовать распарсить error-DTO, но сначала просто лог/exception
        throw IllegalStateException(
            "OpenRouter error ${response.status.value}: $responseBody"
        )
    }

    // тут уже гарантированно успешный ответ с choices [web:29]
    val completion: ChatCompletionResponse = response.body()
    return completion.choices.firstOrNull()?.message?.content.orEmpty()
}