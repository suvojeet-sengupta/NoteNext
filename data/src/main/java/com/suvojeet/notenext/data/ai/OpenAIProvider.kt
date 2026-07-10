package com.suvojeet.notenext.data.ai

import com.suvojeet.notenext.data.remote.Message
import com.suvojeet.notenext.data.remote.OpenAIApiService
import com.suvojeet.notenext.data.remote.OpenAIChatRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAIProvider @Inject constructor(
    private val apiService: com.suvojeet.notenext.data.remote.OpenAIApiService,
    private val settingsRepository: com.suvojeet.notenext.data.repository.SettingsRepository
) : AIProviderService {

    private val mutex = Mutex()
    private var apiKey: String = ""
    private var baseUrl: String = "https://api.openai.com/"

    /**
     * Refreshes apiKey/baseUrl from SettingsRepository before every request.
     * No explicit `initialize()` call needed — the user just enters their key
     * in AI Settings and the provider becomes available immediately.
     */
    private suspend fun refreshConfig() {
        mutex.withLock {
            apiKey = settingsRepository.openAIApiKey.first()
            val url = settingsRepository.openAIBaseUrl.first()
            baseUrl = if (url.endsWith("/")) url else "$url/"
        }
    }

    private val defaultModels = listOf(
        "gpt-5.5",
        "gpt-5.4",
        "gpt-5.4-mini",
        "gpt-5.4-nano",
        "gpt-5-codex",
        "gpt-5.3-codex",
        "gpt-4.1",
        "gpt-4.1-mini",
        "gpt-4.1-nano",
        "gpt-4o",
        "gpt-4o-mini"
    )

    override suspend fun getProviderName(): String = "OpenAI"

    override suspend fun getAvailableModels(): List<String> {
        refreshConfig()
        if (apiKey.isBlank()) return defaultModels
        return try {
            val response = apiService.getModels("Bearer $apiKey")
            response.data.map { it.id }.filter { it.contains("gpt") }
        } catch (e: Exception) {
            defaultModels
        }
    }

    override suspend fun isProviderAvailable(): Boolean {
        refreshConfig()
        return apiKey.isNotBlank()
    }

    override suspend fun summarizeNote(content: String): AIResult<String> {
        return executeWithRetry(listOf(
            Message(role = "system", content = "You are a helpful assistant that summarizes notes concisely."),
            Message(role = "user", content = "Summarize the following note:\n\n$content")
        )) { it.trim() }
    }

    override suspend fun generateChecklist(topic: String): AIResult<List<String>> {
        return executeWithRetry(listOf(
            Message(
                role = "system",
                content = "You are a helpful assistant that generates checklists. Return ONLY a pure JSON array of strings, e.g. [\"Item 1\", \"Item 2\"]. Do not include markdown code blocks or any other text."
            ),
            Message(role = "user", content = "Create a checklist for: $topic")
        )) { content ->
            val cleaned = content.replace("```json", "").replace("```", "").trim()
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                try {
                    Json { ignoreUnknownKeys = true }.decodeFromString(
                        ListSerializer(String.serializer()),
                        cleaned
                    )
                } catch (e: Exception) {
                    content.lines().filter { it.isNotBlank() }.map { it.trim().removePrefix("- ").removePrefix("* ") }
                }
            } else {
                content.lines().filter { it.isNotBlank() }.map { it.trim().removePrefix("- ").removePrefix("* ") }
            }
        }
    }

    override suspend fun generateTodos(input: String): AIResult<List<Pair<String, String>>> {
        return executeWithRetry(listOf(
            Message(
                role = "system",
                content = "You are a helpful assistant that converts paragraphs or messy notes into clear, point-by-point todo tasks. Return ONLY a pure JSON array of objects with 'title' and 'description' keys."
            ),
            Message(role = "user", content = "Convert this into a todo list:\n\n$input")
        )) { content ->
            val cleaned = content.replace("```json", "").replace("```", "").trim()
            try {
                val todoList = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<List<Map<String, String>>>(cleaned)
                todoList.map { it["title"].orEmpty() to it["description"].orEmpty() }
            } catch (e: Exception) {
                content.lines()
                    .filter { it.isNotBlank() }
                    .map { val text = it.trim().removePrefix("- ").removePrefix("* "); text to "" }
            }
        }
    }

    override suspend fun fixGrammar(text: String): AIResult<String> {
        return executeWithRetry(listOf(
            Message(
                role = "system",
                content = "You are a grammar and spelling correction assistant. Fix typos, grammar errors, and improve punctuation. Keep the original meaning and tone intact. Return ONLY the corrected text without any explanations or additional comments."
            ),
            Message(role = "user", content = text)
        )) { it.trim() }
    }

    override suspend fun generateCustomPrompt(systemPrompt: String, userPrompt: String): AIResult<String> {
        return executeWithRetry(listOf(
            Message(role = "system", content = systemPrompt),
            Message(role = "user", content = userPrompt)
        )) { it.trim() }
    }

    private suspend fun <T> executeWithRetry(
        messages: List<com.suvojeet.notenext.data.remote.Message>,
        processor: (String) -> T
    ): AIResult<T> {
        refreshConfig()
        if (apiKey.isBlank()) {
            return AIResult.AuthError("OpenAI API key not set. Add it in AI Settings.")
        }

        val selectedModel = settingsRepository.openAIModel.first()
        val modelsToTry = if (selectedModel.isNotBlank()) {
            listOf(selectedModel) + defaultModels.filter { it != selectedModel }
        } else {
            defaultModels
        }

        var lastException: Exception? = null

        for (model in modelsToTry) {
            var currentRetry = 0
            while (currentRetry <= 2) {
                try {
                    val request = com.suvojeet.notenext.data.remote.OpenAIChatRequest(
                        model = model,
                        messages = messages
                    )
                    val response = apiService.getChatCompletion("Bearer $apiKey", request)
                    
                    if (response.error != null) {
                        lastException = Exception(response.error.message ?: "OpenAI error")
                        break
                    }

                    val content = response.choices?.firstOrNull()?.message?.content
                    if (content != null) {
                        return AIResult.Success(processor(content))
                    } else {
                        lastException = Exception("Empty response from $model")
                        break
                    }
                } catch (e: Exception) {
                    lastException = e
                    val message = e.message ?: ""

                    if (message.contains("429")) {
                        delay(60000L)
                        break
                    }

                    if (message.contains("401")) {
                        return AIResult.AuthError("Invalid OpenAI API key")
                    }

                    if (message.contains("503") || message.contains("502") || e is IOException) {
                        currentRetry++
                        if (currentRetry <= 2) {
                            delay(1000L * currentRetry)
                        }
                    } else {
                        break
                    }
                }
            }
        }

        return AIResult.NetworkError(lastException?.message ?: "Unknown error occurred")
    }
}
