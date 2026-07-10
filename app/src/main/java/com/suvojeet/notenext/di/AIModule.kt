package com.suvojeet.notenext.di

import com.suvojeet.notenext.data.ai.AIFeatureGate
import com.suvojeet.notenext.data.ai.AIProviderManager
import com.suvojeet.notenext.data.ai.AIUsageRepository
import com.suvojeet.notenext.data.ai.AnthropicProvider
import com.suvojeet.notenext.data.ai.GeminiProvider
import com.suvojeet.notenext.data.ai.GroqProvider
import com.suvojeet.notenext.data.ai.OpenAIProvider
import com.suvojeet.notenext.data.remote.AnthropicApiService
import com.suvojeet.notenext.data.remote.GeminiApiService
import com.suvojeet.notenext.data.remote.GroqApiService
import com.suvojeet.notenext.data.remote.OpenAIApiService
import com.suvojeet.notenext.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOpenAIApiService(): OpenAIApiService {
        val contentType = "application/json".toMediaType()
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OpenAIApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAnthropicApiService(): AnthropicApiService {
        val contentType = "application/json".toMediaType()
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(AnthropicApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(): GeminiApiService {
        val contentType = "application/json".toMediaType()
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GeminiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAIProviderManager(
        groqProvider: GroqProvider,
        openAIProvider: OpenAIProvider,
        anthropicProvider: AnthropicProvider,
        geminiProvider: GeminiProvider,
        settingsRepository: SettingsRepository,
        featureGate: AIFeatureGate,
        usageRepository: AIUsageRepository
    ): AIProviderManager {
        return AIProviderManager(
            groqProvider, openAIProvider, anthropicProvider, geminiProvider,
            settingsRepository, featureGate, usageRepository
        )
    }
}
