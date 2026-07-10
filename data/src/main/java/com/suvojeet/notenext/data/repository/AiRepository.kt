package com.suvojeet.notenext.data.repository

import android.util.LruCache
import com.suvojeet.notenext.data.remote.ChatCompletionRequest
import com.suvojeet.notenext.data.remote.GroqApiService
import com.suvojeet.notenext.data.remote.Message
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.first
import com.suvojeet.notenext.data.remote.ModelListResponse
import com.suvojeet.notenext.data.remote.GroqModel
import com.suvojeet.notenext.data.remote.ChatCompletionStreamResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 3.6: Sealed class for classified AI results
 */
sealed class AiResult<out T> {
    data class Success<T>(val data: T) : AiResult<T>()
    data class RateLimited(val retryAfterSeconds: Int) : AiResult<Nothing>()
    object InvalidKey : AiResult<Nothing>()
    data class NetworkError(val message: String) : AiResult<Nothing>()
    object AllModelsFailed : AiResult<Nothing>()
}

inline fun <T> AiResult<T>.onSuccess(action: (T) -> Unit): AiResult<T> {
    if (this is AiResult.Success) action(data)
    return this
}

inline fun <T> AiResult<T>.onFailure(action: (AiResult<Nothing>) -> Unit): AiResult<T> {
    if (this !is AiResult.Success) action(this as AiResult<Nothing>)
    return this
}

/**
 * Maps an [AiResult] failure to a single user-facing message so every AI feature shows
 * consistent error copy. Pass a feature-specific [fallback] for the unexpected/Success case.
 */
fun AiResult<*>.toUserMessage(fallback: String): String = when (this) {
    is AiResult.RateLimited -> "AI is busy. Please try again in ${retryAfterSeconds}s."
    is AiResult.InvalidKey -> "Invalid API key. Check your settings."
    is AiResult.NetworkError -> "Network error: $message"
    is AiResult.AllModelsFailed -> "All AI models failed to respond. Try again later."
    is AiResult.Success -> fallback
}

/**
 * Task 3.3: Per-model rate limit tracking
 */
object GroqRateLimitManager {
    private val modelResetTimes = ConcurrentHashMap<String, Long>()
    private val modelRemainingRequests = ConcurrentHashMap<String, Int>()

    fun update(modelId: String, remaining: Int?, tokens: Int?, retryAfter: Int?) {
        if (remaining != null) modelRemainingRequests[modelId] = remaining
        if (retryAfter != null) {
            modelResetTimes[modelId] = System.currentTimeMillis() + (retryAfter * 1000L)
        } else if (remaining != null && remaining == 0) {
            // If we hit 0 remaining without a retry-after, assume a 60s block
            modelResetTimes[modelId] = System.currentTimeMillis() + 60000L
        }
    }

    fun isRateLimited(modelId: String): Boolean {
        val resetTime = modelResetTimes[modelId] ?: 0L
        if (System.currentTimeMillis() < resetTime) return true
        val remaining = modelRemainingRequests[modelId] ?: return false
        return remaining <= 1
    }

    fun getRetryAfter(modelId: String): Int {
        val resetTime = modelResetTimes[modelId] ?: 0L
        val seconds = ((resetTime - System.currentTimeMillis()) / 1000).toInt()
        return seconds.coerceAtLeast(0)
    }
}

@Singleton
class AiRepository @Inject constructor(
    private val apiService: GroqApiService,
    private val settingsRepository: SettingsRepository,
    private val aiProviderManager: com.suvojeet.notenext.data.ai.AIProviderManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    // In-memory caches for AI responses
    private val summaryCache = LruCache<String, String>(50)
    private val checklistCache = LruCache<String, List<String>>(50)
    private val grammarCache = LruCache<String, String>(50)

    // Task 3.4: Request deduplication map
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<AiResult<*>>>()
    private val requestMutex = Mutex()

    /**
     * Task 2: Active model lists for 2025
     */
    private val fastModels = listOf(
        "llama-3.1-8b-instant",
        "qwen/qwen3-32b",
        "meta-llama/llama-4-scout-17b-16e-instruct",
        "llama-3.3-70b-versatile"
    )

    private val largeModels = listOf(
        "llama-3.3-70b-versatile",
        "meta-llama/llama-4-scout-17b-16e-instruct",
        "qwen/qwen3-32b",
        "llama-3.1-8b-instant"
    )

    private suspend fun getTargetModels(isFast: Boolean): List<String> {
        val useCustom = settingsRepository.useCustomGroqKey.first()
        return if (useCustom) {
            val customModel = if (isFast) {
                settingsRepository.customFastModel.first()
            } else {
                settingsRepository.customLargeModel.first()
            }
            if (customModel.isNotBlank()) {
                listOf(customModel)
            } else {
                if (isFast) fastModels else largeModels
            }
        } else {
            if (isFast) fastModels else largeModels
        }
    }

    suspend fun fetchAvailableModels(): Result<List<GroqModel>> {
        return try {
            val response = apiService.getModels()
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Task 3: Executes the API request with model fallback, rate-limit awareness, and smarter retries.
     */
    private suspend fun <T> executeWithRetry(
        isFast: Boolean,
        messages: List<Message>,
        maxRetriesPerModel: Int = 2,
        processor: (String) -> T
    ): AiResult<T> {
        val models = getTargetModels(isFast)
        var lastException: Exception? = null

        for (model in models) {
            // Task 3.1 & 3.3: Rate limit awareness
            if (GroqRateLimitManager.isRateLimited(model)) continue

            var currentRetry = 0
            while (currentRetry <= maxRetriesPerModel) {
                try {
                    val request = ChatCompletionRequest(
                        model = model,
                        messages = messages
                    )
                    val response = apiService.getChatCompletion(request)
                    val content = response.choices.firstOrNull()?.message?.content
                    
                    if (content != null) {
                        return AiResult.Success(processor(content))
                    } else {
                        lastException = Exception("Empty response from $model")
                        break
                    }
                } catch (e: Exception) {
                    lastException = e
                    val message = e.message ?: ""
                    
                    // Task 3.2: Error classification and smart retries
                    if (message.contains("429")) {
                        val retryAfter = GroqRateLimitManager.getRetryAfter(model).coerceAtLeast(60)
                        GroqRateLimitManager.update(model, remaining = 0, tokens = null, retryAfter = retryAfter)
                        break // try next model
                    }
                    
                    if (message.contains("401")) {
                        return AiResult.InvalidKey
                    }
                    
                    if (message.contains("503") || message.contains("502") || e is IOException) {
                        // Retry for server or network errors
                        currentRetry++
                        if (currentRetry <= maxRetriesPerModel) {
                            delay(1000L * currentRetry)
                        }
                    } else {
                        // Other errors, move to next model
                        break
                    }
                }
            }
        }

        return lastException?.let {
            AiResult.NetworkError(it.message ?: "Unknown error occurred")
        } ?: AiResult.AllModelsFailed
    }

    private fun executeWithRetryStream(
        isFast: Boolean,
        messages: List<Message>,
        maxRetriesPerModel: Int = 2
    ): Flow<AiResult<String>> = flow {
        val models = getTargetModels(isFast)
        var lastException: Exception? = null

        for (model in models) {
            if (GroqRateLimitManager.isRateLimited(model)) continue

            var currentRetry = 0
            while (currentRetry <= maxRetriesPerModel) {
                try {
                    val request = ChatCompletionRequest(
                        model = model,
                        messages = messages,
                        stream = true
                    )
                    val responseBody = apiService.getChatCompletionStream(request)
                    val accumulated = StringBuilder()
                    
                    responseBody.byteStream().bufferedReader().useLines { lines ->
                        for (line in lines) {
                            if (line.startsWith("data: ")) {
                                val data = line.substring(6).trim()
                                if (data == "[DONE]") break
                                
                                try {
                                    val streamResponse = json.decodeFromString<ChatCompletionStreamResponse>(data)
                                    val content = streamResponse.choices.firstOrNull()?.delta?.content
                                    if (content != null) {
                                        accumulated.append(content)
                                        emit(AiResult.Success(accumulated.toString()))
                                    }
                                } catch (e: Exception) {
                                    // Ignore parse errors for partial chunks
                                }
                            }
                        }
                    }
                    return@flow
                } catch (e: Exception) {
                    lastException = e
                    val message = e.message ?: ""
                    
                    if (message.contains("429")) {
                        val retryAfter = GroqRateLimitManager.getRetryAfter(model).coerceAtLeast(60)
                        GroqRateLimitManager.update(model, remaining = 0, tokens = null, retryAfter = retryAfter)
                        break
                    }
                    
                    if (message.contains("401")) {
                        emit(AiResult.InvalidKey)
                        return@flow
                    }
                    
                    if (message.contains("503") || message.contains("502") || e is IOException) {
                        currentRetry++
                        if (currentRetry <= maxRetriesPerModel) {
                            delay(1000L * currentRetry)
                        }
                    } else {
                        break
                    }
                }
            }
        }

        lastException?.let {
            emit(AiResult.NetworkError(it.message ?: "Unknown error occurred"))
        } ?: emit(AiResult.AllModelsFailed)
    }

    /**
     * Task 3.4: Prevents duplicate in-flight requests for the same content.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> deduplicate(
        key: String,
        block: suspend () -> AiResult<T>
    ): AiResult<T> = coroutineScope {
        val deferred = requestMutex.withLock {
            val existing = inFlightRequests[key]
            if (existing != null) {
                existing
            } else {
                val newDeferred = async {
                    try {
                        block()
                    } finally {
                        inFlightRequests.remove(key)
                    }
                }
                inFlightRequests[key] = newDeferred
                newDeferred
            }
        }
        deferred.await() as AiResult<T>
    }

    fun summarizeNote(content: String): Flow<AiResult<String>> = flow {
        summaryCache.get(content)?.let {
            emit(AiResult.Success(it))
            return@flow
        }

        val wordCount = content.split("\\s+".toRegex()).size
        val isFast = wordCount < 1000

        val messages = listOf(
            Message(role = "system", content = "You are a helpful assistant that summarizes notes concisely."),
            Message(role = "user", content = "Summarize the following note:\n\n$content")
        )

        var finalSummary = ""
        emitAll(
            executeWithRetryStream(isFast, messages)
                .onEach { result ->
                    if (result is AiResult.Success) {
                        finalSummary = result.data
                    }
                }
        )
        
        if (finalSummary.isNotEmpty()) {
            summaryCache.put(content, finalSummary)
        }
    }

    fun generateChecklist(topic: String): Flow<AiResult<List<String>>> = flow {
        checklistCache.get(topic)?.let {
            emit(AiResult.Success(it))
            return@flow
        }

        val result = deduplicate("checklist_${topic.hashCode()}") {
            val messages = listOf(
                Message(
                    role = "system", 
                    content = "You are a helpful assistant that generates checklists. Return ONLY a pure JSON array of strings, e.g. [\"Item 1\", \"Item 2\"]. Do not include markdown code blocks or any other text."
                ),
                Message(role = "user", content = "Create a checklist for: $topic")
            )

            executeWithRetry(false, messages) { content ->
                val cleaned = content.replace("```json", "").replace("```", "").trim()
                if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                    try {
                        json.decodeFromString(ListSerializer(String.serializer()), cleaned)
                    } catch (e: Exception) {
                        content.lines().filter { it.isNotBlank() }.map { it.trim().removePrefix("- ").removePrefix("* ") }
                    }
                } else {
                    content.lines().filter { it.isNotBlank() }.map { it.trim().removePrefix("- ").removePrefix("* ") }
                }
            }
        }
        
        if (result is AiResult.Success) {
            checklistCache.put(topic, result.data)
        }
        emit(result)
    }

    fun generateTodos(input: String): Flow<AiResult<List<Pair<String, String>>>> = flow {
        val result = deduplicate("todos_${input.hashCode()}") {
            val messages = listOf(
                Message(
                    role = "system", 
                    content = "You are a helpful assistant that converts paragraphs or messy notes into clear, point-by-point todo tasks. For each task, provide a concise title and a short description if needed. Return ONLY a pure JSON array of objects, each with 'title' and 'description' keys. Example: [{\"title\": \"Buy milk\", \"description\": \"Get full cream milk from store\"}, {\"title\": \"Call mom\", \"description\": \"Wish her happy birthday\"}]. Do not include markdown code blocks or any other text."
                ),
                Message(role = "user", content = "Convert this into a todo list:\n\n$input")
            )

            executeWithRetry(false, messages) { content ->
                val cleaned = content.replace("```json", "").replace("```", "").trim()
                try {
                    val todoList = json.decodeFromString<List<Map<String, String>>>(cleaned)
                    todoList.map { 
                        it["title"].orEmpty() to it["description"].orEmpty()
                    }
                } catch (e: Exception) {
                    content.lines()
                        .filter { it.isNotBlank() }
                        .map { 
                            val text = it.trim().removePrefix("- ").removePrefix("* ")
                            text to ""
                        }
                }
            }
        }
        emit(result)
    }

    fun fixGrammar(text: String): Flow<AiResult<String>> = flow {
        grammarCache.get(text)?.let {
            emit(AiResult.Success(it))
            return@flow
        }

        val result = deduplicate("grammar_${text.hashCode()}") {
            val messages = listOf(
                Message(
                    role = "system", 
                    content = "You are a grammar and spelling correction assistant. Fix typos, grammar errors, and improve punctuation. Keep the original meaning and tone intact. Return ONLY the corrected text without any explanations or additional comments."
                ),
                Message(role = "user", content = text)
            )

            executeWithRetry(true, messages) { it.trim() }
        }
        
        if (result is AiResult.Success) {
            grammarCache.put(text, result.data)
        }
        emit(result)
    }
}
