package com.suvojeet.notenext.data.remote

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GroqApiService {
    @Headers("Content-Type: application/json")
    @POST("openai/v1/chat/completions")
    suspend fun getChatCompletion(
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}
