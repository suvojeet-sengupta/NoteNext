package com.suvojeet.notenext.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming
import okhttp3.ResponseBody

interface GroqApiService {
    @Headers("Content-Type: application/json")
    @POST("openai/v1/chat/completions")
    suspend fun getChatCompletion(
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @Headers("Content-Type: application/json", "Accept: text/event-stream")
    @POST("openai/v1/chat/completions")
    @Streaming
    suspend fun getChatCompletionStream(
        @Body request: ChatCompletionRequest
    ): ResponseBody

    @GET("openai/v1/models")
    suspend fun getModels(): ModelListResponse
}
