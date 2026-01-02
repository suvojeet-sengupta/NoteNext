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
    fun generateChecklist(topic: String): Flow<Result<List<String>>> = flow {
        val model = "llama-3.3-70b-versatile"
        val messages = listOf(
            Message(role = "system", content = "You are a helpful assistant that generates checklists. Return ONLY a pure JSON array of strings, e.g. [\"Item 1\", \"Item 2\"]. Do not include markdown code blocks or any other text."),
            Message(role = "user", content = "Create a checklist for: $topic")
        )

        try {
            val request = ChatCompletionRequest(
                model = model,
                messages = messages
            )
            val response = apiService.getChatCompletion(request)
            val content = response.choices.firstOrNull()?.message?.content
            
            if (content != null) {
                // Try to parse JSON array manually or via Gson if available
                // Simplistic parsing for now: remove brackets and split
                val cleaned = content.replace("```json", "").replace("```", "").trim()
                if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                    val items = com.google.gson.Gson().fromJson(cleaned, Array<String>::class.java).toList()
                    emit(Result.success(items))
                } else {
                    // Fallback if not JSON: split by newlines
                    val items = content.lines().filter { it.isNotBlank() }.map { it.trim().removePrefix("- ").removePrefix("* ") }
                    emit(Result.success(items))
                }
            } else {
                emit(Result.failure(Exception("Empty response from $model")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
