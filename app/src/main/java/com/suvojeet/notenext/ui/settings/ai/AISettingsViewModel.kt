package com.suvojeet.notenext.ui.settings.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.ai.AIFeature
import com.suvojeet.notenext.data.ai.AIFeatureGate
import com.suvojeet.notenext.data.ai.AIProvider
import com.suvojeet.notenext.data.ai.AIProviderManager
import com.suvojeet.notenext.data.ai.AIUsageRepository
import com.suvojeet.notenext.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the unified AI Settings hub.
 *
 * The hub is the single entry point users land on when they tap "AI" in the
 * main Settings screen. From here they can:
 *  - flip the master kill-switch (one toggle disables every AI feature instantly)
 *  - pick which provider to use
 *  - enter API keys for any provider (AI is bundled — optional override)
 *  - jump to the per-feature toggles screen
 *  - jump to the usage dashboard
 */
@HiltViewModel
class AISettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiProviderManager: AIProviderManager,
    private val featureGate: AIFeatureGate,
    private val usageRepository: AIUsageRepository
) : ViewModel() {

    val masterEnabled: StateFlow<Boolean> = settingsRepository.aiMasterEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val usageTrackingEnabled: StateFlow<Boolean> = settingsRepository.aiUsageTrackingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _selectedProvider = MutableStateFlow(AIProvider.GROQ)
    val selectedProvider: StateFlow<AIProvider> = _selectedProvider.asStateFlow()

    val groqCustomKey: StateFlow<String> = settingsRepository.customGroqKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val useCustomGroqKey: StateFlow<Boolean> = settingsRepository.useCustomGroqKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val groqFastModel: StateFlow<String> = settingsRepository.customFastModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "llama-3.1-8b-instant")
    val groqLargeModel: StateFlow<String> = settingsRepository.customLargeModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "llama-3.3-70b-versatile")

    val openAIKey: StateFlow<String> = settingsRepository.openAIApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val openAIBaseUrl: StateFlow<String> = settingsRepository.openAIBaseUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://api.openai.com/")
    val openAIModel: StateFlow<String> = settingsRepository.openAIModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gpt-4o-mini")

    val anthropicKey: StateFlow<String> = settingsRepository.anthropicApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val anthropicModel: StateFlow<String> = settingsRepository.anthropicModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "claude-3-5-sonnet-20241022")

    val geminiKey: StateFlow<String> = settingsRepository.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val geminiModel: StateFlow<String> = settingsRepository.geminiModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-3.1-flash")

    private val _availableModels = MutableStateFlow<Map<AIProvider, List<String>>>(emptyMap())
    val availableModels: StateFlow<Map<AIProvider, List<String>>> = _availableModels.asStateFlow()

    private val _isRefreshingModels = MutableStateFlow(false)
    val isRefreshingModels: StateFlow<Boolean> = _isRefreshingModels.asStateFlow()

    val totalInvocations: StateFlow<Int> = usageRepository.observeTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            _selectedProvider.value = aiProviderManager.getActiveProvider()
            refreshModels()
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveAiMasterEnabled(enabled)
        }
    }

    fun setUsageTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveAiUsageTrackingEnabled(enabled)
        }
    }

    fun selectProvider(provider: AIProvider) {
        viewModelScope.launch {
            _selectedProvider.value = provider
            aiProviderManager.setActiveProvider(provider)
        }
    }

    fun saveGroqKey(key: String) = viewModelScope.launch {
        settingsRepository.saveCustomGroqKey(key)
        if (key.isNotBlank()) settingsRepository.saveUseCustomGroqKey(true)
        refreshModels()
    }

    fun setUseCustomGroqKey(use: Boolean) = viewModelScope.launch {
        settingsRepository.saveUseCustomGroqKey(use)
        refreshModels()
    }

    fun saveGroqFastModel(model: String) = viewModelScope.launch {
        settingsRepository.saveCustomFastModel(model)
    }

    fun saveGroqLargeModel(model: String) = viewModelScope.launch {
        settingsRepository.saveCustomLargeModel(model)
    }

    fun saveOpenAIKey(key: String) = viewModelScope.launch {
        settingsRepository.saveOpenAIApiKey(key)
        refreshModels()
    }

    fun saveOpenAIBaseUrl(url: String) = viewModelScope.launch {
        settingsRepository.saveOpenAIBaseUrl(url)
    }

    fun saveOpenAIModel(model: String) = viewModelScope.launch {
        settingsRepository.saveOpenAIModel(model)
    }

    fun saveAnthropicKey(key: String) = viewModelScope.launch {
        settingsRepository.saveAnthropicApiKey(key)
    }

    fun saveAnthropicModel(model: String) = viewModelScope.launch {
        settingsRepository.saveAnthropicModel(model)
    }

    fun saveGeminiKey(key: String) = viewModelScope.launch {
        settingsRepository.saveGeminiApiKey(key)
    }

    fun saveGeminiModel(model: String) = viewModelScope.launch {
        settingsRepository.saveGeminiModel(model)
    }

    fun refreshModels() = viewModelScope.launch {
        _isRefreshingModels.value = true
        val map = mutableMapOf<AIProvider, List<String>>()
        listOf(
            AIProvider.GROQ,
            AIProvider.OPENAI,
            AIProvider.ANTHROPIC,
            AIProvider.GEMINI
        ).forEach { provider ->
            try {
                map[provider] = aiProviderManager.getProviderService(provider)?.getAvailableModels().orEmpty()
            } catch (_: Exception) {
                map[provider] = emptyList()
            }
        }
        _availableModels.value = map
        _isRefreshingModels.value = false
    }
}
