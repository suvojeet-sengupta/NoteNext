package com.suvojeet.notenext.credits

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.IOException

object CreditsProvider {
    private val json = Json { ignoreUnknownKeys = true }

    fun getCredits(context: Context): CreditsData? {
        return try {
            val jsonString = context.assets.open("credits.json").bufferedReader().use { it.readText() }
            json.decodeFromString<CreditsData>(jsonString)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
