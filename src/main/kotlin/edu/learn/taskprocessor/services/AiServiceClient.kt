package edu.learn.taskprocessor.services

interface AiServiceClient {
    /**
     * Выполняет запрос к AI и возвращает готовый текст.
     *
     * Внутри может использовать Ktor HttpClient, async/await и т.п.
     */
    suspend fun generateText(prompt: String): String
}