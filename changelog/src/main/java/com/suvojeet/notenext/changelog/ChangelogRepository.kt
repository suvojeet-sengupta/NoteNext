package com.suvojeet.notenext.changelog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object ChangelogRepository {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    private const val CHANGELOG_URL = "https://raw.githubusercontent.com/suvojeet-sengupta/NoteNext/main/changelog.json"

    suspend fun getChangelog(context: Context? = null): Result<ChangelogList> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(CHANGELOG_URL)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext loadFromAssets(context) ?: Result.failure(IOException("Unexpected code $response"))
                }

                val body = response.body.string()
                if (body.isEmpty()) {
                    return@withContext loadFromAssets(context) ?: Result.failure(IOException("Empty body"))
                }
                val data = json.decodeFromString<ChangelogList>(body)
                Result.success(data)
            }
        } catch (e: Exception) {
            loadFromAssets(context) ?: Result.failure(e)
        }
    }

    private fun loadFromAssets(context: Context?): Result<ChangelogList>? {
        if (context == null) return null
        return try {
            val jsonString = context.assets.open("changelog.json").bufferedReader().use { it.readText() }
            val data = json.decodeFromString<ChangelogList>(jsonString)
            Result.success(data)
        } catch (e: Exception) {
            null
        }
    }
}
