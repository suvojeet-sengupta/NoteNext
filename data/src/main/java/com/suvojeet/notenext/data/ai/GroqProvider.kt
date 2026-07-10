package com.suvojeet.notenext.data.ai

import com.suvojeet.notenext.data.remote.ChatCompletionRequest
import com.suvojeet.notenext.data.remote.GroqApiService
import com.suvojeet.notenext.data.remote.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqProvider @Inject constructor(
    private val groqApiService: GroqApiService,
    private val settingsRepository: com.suvojeet.notenext.data.repository.SettingsRepository
) : AIProviderService {

    private val fastModels = listOf(
        "llama-3.1-8b-instant",
        "qwen/qwen3-32b",
        "meta-llama/llama-4-scout-17b-16e-instruct",
        "llama-3.3-70b-versatile"
    )

    private val largeModels = listOf(
        "llama-3.3-70b-versatile",
        "meta-llama/llama-4-scout-17b-16e-instruct",
        "qwen/qwen3-32b"
    )

    override suspend fun getProviderName(): String = "Groq"

    override suspend fun getAvailableModels(): List<String> {
        return try {
            val response = groqApiService.getModels()
            response.data.map { it.id }.sorted()
        } catch (e: Exception) {
            fastModels
        }
    }

    /**
     * Groq is always available because the app ships with a built-in
     * encrypted API key (see NetworkModule.authInterceptor). The user can
     * override it with their own key via Settings → AI → Providers.
     */
    override suspend fun isProviderAvailable(): Boolean = true

    override suspend fun summarizeNote(content: String): AIResult<String> {
        return executeWithRetry(content.split("\\s+".toRegex()).size < 1000, listOf(
            Message(role = "system", content = "You are a helpful assistant that summarizes notes concisely."),
            Message(role = "user", content = "Summarize the following note:\n\n$content")
        )) { it.trim() }
    }

    override suspend fun generateChecklist(topic: String): AIResult<List<String>> {
        return executeWithRetry(false, listOf(
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
        return executeWithRetry(false, listOf(
            Message(
                role = "system",
                content = "You are a helpful assistant that converts paragraphs or messy notes into clear, point-by-point todo tasks. For each task, provide a concise title and a short description if needed. Return ONLY a pure JSON array of objects, each with 'title' and 'description' keys."
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
        return executeWithRetry(true, listOf(
            Message(
                role = "system",
                content = "You are a grammar and spelling correction assistant. Fix typos, grammar errors, and improve punctuation. Keep the original meaning and tone intact. Return ONLY the corrected text without any explanations or additional comments."
            ),
            Message(role = "user", content = text)
        )) { it.trim() }
    }

    override suspend fun generateCustomPrompt(systemPrompt: String, userPrompt: String): AIResult<String> {
        return executeWithRetry(false, listOf(
            Message(role = "system", content = systemPrompt),
            Message(role = "user", content = userPrompt)
        )) { it.trim() }
    }

    private suspend fun <T> executeWithRetry(
        isFast: Boolean,
        messages: List<Message>,
        processor: (String) -> T
    ): AIResult<T> {
        // Auth handled by NetworkModule.authInterceptor — uses the user's
        // custom key when set, falls back to the built-in BuildConfig key.

        val customModel = if (isFast) {
            settingsRepository.customFastModel.first()
        } else {
            settingsRepository.customLargeModel.first()
        }

        val baseModels = if (isFast) fastModels else largeModels
        val models = if (customModel.isNotBlank()) {
            listOf(customModel) + baseModels.filter { it != customModel }
        } else {
            baseModels
        }

        var lastException: Exception? = null

        for (model in models) {
            var currentRetry = 0
            while (currentRetry <= 2) {
                try {
                    val request = ChatCompletionRequest(model = model, messages = messages)
                    val response = groqApiService.getChatCompletion(request)
                    val content = response.choices.firstOrNull()?.message?.content

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
                        return AIResult.AuthError("Invalid API key")
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
