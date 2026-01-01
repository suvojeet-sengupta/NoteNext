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
        // Word count logic to select model list
        val wordCount = content.split("\\s+".toRegex()).size
        
        val models = if (wordCount < 1000) {
            listOf(
                "llama-3.1-8b-instant",
                "gemma2-9b-it",
                "mixtral-8x7b-32768",
                "llama-3.3-70b-versatile",
                "llama-3.1-70b-versatile"
            )
        } else {
            listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-70b-versatile",
                "mixtral-8x7b-32768",
                "gemma2-9b-it",
                "llama-3.1-8b-instant"
            )
        }

        val messages = listOf(
            Message(role = "system", content = "You are a helpful assistant that summarizes notes concisely."),
            Message(role = "user", content = "Summarize the following note:\n\n$content")
        )

        var lastException: Exception? = null
        var success = false

        for (model in models) {
            try {
                // Log attempting model? (Optional)
                val request = ChatCompletionRequest(
                    model = model,
                    messages = messages
                )
                val response = apiService.getChatCompletion(request)
                val summary = response.choices.firstOrNull()?.message?.content
                
                if (summary != null) {
                    emit(Result.success(summary))
                    success = true
                    break // Exit loop on success
                } else {
                    lastException = Exception("Empty response from $model")
                }
            } catch (e: Exception) {
                lastException = e
                // Continue to next model
            }
        }

        if (!success) {
            emit(Result.failure(lastException ?: Exception("Unknown error during summarization")))
        }
    }
}
