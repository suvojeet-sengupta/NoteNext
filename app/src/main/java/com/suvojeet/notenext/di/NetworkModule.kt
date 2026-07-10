package com.suvojeet.notenext.di

import com.suvojeet.notenext.BuildConfig
import com.suvojeet.notenext.data.remote.GroqApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        groqConfigProvider: com.suvojeet.notenext.data.remote.GroqConfigProvider
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.suvojeet.notenext.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val authInterceptor = Interceptor { chain ->
            val config = groqConfigProvider.config

            val apiKey = if (config.useCustomKey && config.customKey.isNotBlank()) {
                config.customKey
            } else {
                val encryptedKey = com.suvojeet.notenext.BuildConfig.GROQ_API_KEY_ENC
                val xorKey = com.suvojeet.notenext.BuildConfig.GROQ_XOR_KEY
                
                if (encryptedKey.isNotBlank()) {
                    try {
                        val decoded = android.util.Base64.decode(encryptedKey, android.util.Base64.DEFAULT)
                        val decrypted = decoded.map { (it.toInt() xor xorKey.toInt()).toByte() }.toByteArray()
                        String(decrypted)
                    } catch (e: Exception) {
                        ""
                    }
                } else {
                    ""
                }
            }

            val request = if (apiKey.isNotBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val timeoutInterceptor = Interceptor { chain ->
            val request = chain.request()
            val modelId = try {
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                val bodyString = buffer.readUtf8()
                val match = "\"model\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(bodyString)
                match?.groupValues?.get(1)
            } catch (e: Exception) {
                null
            }

            // Task 3.5: Timeout differentiation
            val timeout = when (modelId) {
                "llama-3.1-8b-instant", "qwen/qwen3-32b" -> 15
                else -> 30
            }

            chain.withConnectTimeout(timeout, TimeUnit.SECONDS)
                .withReadTimeout(timeout, TimeUnit.SECONDS)
                .withWriteTimeout(timeout, TimeUnit.SECONDS)
                .proceed(request)
        }

        val rateLimitInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            
            // Task 3.1: Capture rate limit headers
            val modelId = try {
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                val bodyString = buffer.readUtf8()
                val match = "\"model\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(bodyString)
                match?.groupValues?.get(1)
            } catch (e: Exception) {
                null
            }
            
            val remainingRequests = response.header("x-ratelimit-remaining-requests")?.toIntOrNull()
            val remainingTokens = response.header("x-ratelimit-remaining-tokens")?.toIntOrNull()
            val retryAfter = response.header("retry-after")?.toIntOrNull()
            
            if (modelId != null) {
                com.suvojeet.notenext.data.repository.GroqRateLimitManager.update(
                    modelId, 
                    remainingRequests, 
                    remainingTokens, 
                    retryAfter
                )
            }
            
            response
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(timeoutInterceptor)
            .addInterceptor(rateLimitInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideGroqApiService(retrofit: Retrofit): GroqApiService {
        return retrofit.create(GroqApiService::class.java)
    }
}
