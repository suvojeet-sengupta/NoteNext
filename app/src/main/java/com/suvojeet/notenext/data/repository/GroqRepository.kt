package com.suvojeet.notenext.data.repository

import com.suvojeet.notenext.data.remote.ChatCompletionRequest
import com.suvojeet.notenext.data.remote.GroqApiService
import com.suvojeet.notenext.data.remote.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqRepository @Inject constructor(
    private val apiService: GroqApiService
) {
    fun summarizeNote(content: String): Flow<Result<String>> = flow {
        // Word count logic to select model
        val wordCount = content.split("\\s+".toRegex()).size
        val model = if (wordCount < 1000) {
            "llama-3.1-8b-instant"
        } else {
            "llama-3.3-70b-versatile"
        }

        val messages = listOf(
            Message(role = "system", content = "You are a helpful assistant that summarizes notes concisely."),
            Message(role = "user", content = "Summarize the following note:\n\n$content")
        )

        val request = ChatCompletionRequest(
            model = model,
            messages = messages
        )

        try {
            val response = apiService.getChatCompletion(request)
            val summary = response.choices.firstOrNull()?.message?.content
            if (summary != null) {
                emit(Result.success(summary))
            } else {
                emit(Result.failure(Exception("Empty response from AI")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
