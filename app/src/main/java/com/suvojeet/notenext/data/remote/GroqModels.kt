package com.suvojeet.notenext.data.remote

data class ChatCompletionRequest(
    val model: String,
    val messages: List_Message>,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val id: String,
    val choices: List_Choice>
)

data class Choice(
    val index: Int,
    val message: Message
)
