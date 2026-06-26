package edu.learn.taskprocessor.dto.openrouter

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>
) {
    @Serializable
    data class Choice(
        val message: ChatMessage
    )
}
