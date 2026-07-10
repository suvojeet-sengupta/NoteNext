package com.suvojeet.notenext.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val index: Int,
    val message: Message
)

@Serializable
data class ChatCompletionStreamResponse(
    val id: String? = null,
    val choices: List<StreamChoice> = emptyList()
)

@Serializable
data class StreamChoice(
    val index: Int? = null,
    val delta: StreamDelta? = null,
    val finish_reason: String? = null
)

@Serializable
data class StreamDelta(
    val content: String? = null
)

@Serializable
data class ModelListResponse(
    val data: List<GroqModel>
)

@Serializable
data class GroqModel(
    val id: String,
    val created: Long,
    val owned_by: String
)
