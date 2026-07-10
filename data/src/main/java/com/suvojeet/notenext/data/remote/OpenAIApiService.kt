package com.suvojeet.notenext.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAIApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") auth: String,
        @Body request: OpenAIChatRequest
    ): OpenAIChatResponse

    @GET("v1/models")
    suspend fun getModels(
        @Header("Authorization") auth: String
    ): OpenAIModelListResponse
}

@Serializable
data class OpenAIChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null
)

@Serializable
data class OpenAIChatResponse(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<OpenAIChoice>? = null,
    val usage: OpenAIUsage? = null,
    val error: OpenAIError? = null
)

@Serializable
data class OpenAIChoice(
    val index: Int? = null,
    val message: Message? = null,
    val finish_reason: String? = null
)

@Serializable
data class OpenAIUsage(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null
)

@Serializable
data class OpenAIError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

@Serializable
data class OpenAIModelListResponse(
    val data: List<OpenAIModel>
)

@Serializable
data class OpenAIModel(
    val id: String,
    val created: Long,
    val owned_by: String
)
