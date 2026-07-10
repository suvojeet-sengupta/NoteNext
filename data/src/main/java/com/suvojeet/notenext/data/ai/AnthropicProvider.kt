package com.suvojeet.notenext.data.ai

import com.suvojeet.notenext.data.remote.AnthropicApiService
import com.suvojeet.notenext.data.remote.AnthropicMessage
import com.suvojeet.notenext.data.remote.AnthropicMessageRequest
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
class AnthropicProvider @Inject constructor(
    private val apiService: AnthropicApiService,
    private val settingsRepository: com.suvojeet.notenext.data.repository.SettingsRepository
) : AIProviderService {

    private val mutex = Mutex()
    private var apiKey: String = ""

    private val defaultModels = listOf(
        "claude-opus-4.7",
        "claude-opus-4.6",
        "claude-sonnet-4.6",
        "claude-haiku-4.5"
    )

    private suspend fun refreshConfig() {
        mutex.withLock {
            apiKey = settingsRepository.anthropicApiKey.first()
        }
    }

    override suspend fun getProviderName(): String = "Anthropic (Claude)"

    override suspend fun getAvailableModels(): List<String> = defaultModels

    override suspend fun isProviderAvailable(): Boolean {
        refreshConfig()
        return apiKey.isNotBlank()
    }

    override suspend fun summarizeNote(content: String): AIResult<String> {
        return executeWithRetry(
            systemPrompt = "You are a helpful assistant that summarizes notes concisely.",
            userPrompt = "Summarize the following note:\n\n$content"
        ) { it.trim() }
    }

    override suspend fun generateChecklist(topic: String): AIResult<List<String>> {
        return executeWithRetry(
            systemPrompt = "You are a helpful assistant that generates checklists. Return ONLY a pure JSON array of strings, e.g. [\"Item 1\", \"Item 2\"]. Do not include markdown code blocks or any other text.",
            userPrompt = "Create a checklist for: $topic"
        ) { content ->
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
        return executeWithRetry(
            systemPrompt = "You are a helpful assistant that converts paragraphs or messy notes into clear, point-by-point todo tasks. Return ONLY a pure JSON array of objects with 'title' and 'description' keys.",
            userPrompt = "Convert this into a todo list:\n\n$input"
        ) { content ->
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
        return executeWithRetry(
            systemPrompt = "You are a grammar and spelling correction assistant. Fix typos, grammar errors, and improve punctuation. Keep the original meaning and tone intact. Return ONLY the corrected text without any explanations or additional comments.",
            userPrompt = text
        ) { it.trim() }
    }

    override suspend fun generateCustomPrompt(systemPrompt: String, userPrompt: String): AIResult<String> {
        return executeWithRetry(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt
        ) { it.trim() }
    }

    private suspend fun <T> executeWithRetry(
        systemPrompt: String,
        userPrompt: String,
        processor: (String) -> T
    ): AIResult<T> {
        refreshConfig()
        if (apiKey.isBlank()) {
            return AIResult.AuthError("Anthropic API key not set. Add it in AI Settings.")
        }

        val selectedModel = settingsRepository.anthropicModel.first()
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
                    val request = AnthropicMessageRequest(
                        model = model,
                        messages = listOf(
                            AnthropicMessage(role = "user", content = userPrompt)
                        ),
                        system = systemPrompt,
                        max_tokens = 4096
                    )

                    val response = apiService.createMessage(apiKey, "2023-06-01", request)
                    
                    if (response.error != null) {
                        lastException = Exception(response.error.message ?: "Anthropic error")
                        break
                    }

                    val content = response.content?.firstOrNull()?.text
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
                        return AIResult.AuthError("Invalid Anthropic API key")
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
