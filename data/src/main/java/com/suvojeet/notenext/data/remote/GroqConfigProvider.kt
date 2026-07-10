package com.suvojeet.notenext.data.remote

import com.suvojeet.notenext.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqConfigProvider @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _config = AtomicReference(Config())
    val config: Config get() = _config.get()

    data class Config(
        val useCustomKey: Boolean = false,
        val customKey: String = ""
    )

    init {
        combine(
            settingsRepository.useCustomGroqKey,
            settingsRepository.customGroqKey
        ) { use, key ->
            Config(use, key)
        }.onEach { 
            _config.set(it)
        }.launchIn(scope)
    }
}
