package com.suvojeet.notenext.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface AnthropicApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") apiVersion: String = "2023-06-01",
        @Body request: AnthropicMessageRequest
    ): AnthropicMessageResponse
}

@Serializable
data class AnthropicMessageRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    val max_tokens: Int = 4096,
    val system: String? = null,
    val temperature: Double = 0.7
)

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String
)

@Serializable
data class AnthropicMessageResponse(
    val id: String? = null,
    val type: String? = null,
    val role: String? = null,
    val content: List<AnthropicContentBlock>? = null,
    val model: String? = null,
    val stop_reason: String? = null,
    val error: AnthropicError? = null
)

@Serializable
data class AnthropicContentBlock(
    val type: String? = null,
    val text: String? = null
)

@Serializable
data class AnthropicError(
    val type: String? = null,
    val message: String? = null
)
