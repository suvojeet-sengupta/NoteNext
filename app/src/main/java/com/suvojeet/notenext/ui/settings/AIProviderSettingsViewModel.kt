package com.suvojeet.notenext.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.ai.AIProvider
import com.suvojeet.notenext.data.ai.AIProviderManager
import com.suvojeet.notenext.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIProviderSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiProviderManager: AIProviderManager
) : ViewModel() {

    private val _selectedProvider = MutableStateFlow<AIProvider>(AIProvider.GROQ)
    val selectedProvider: StateFlow<AIProvider> = _selectedProvider.asStateFlow()

    private val _groqKey = MutableStateFlow("")
    val groqKey: StateFlow<String> = _groqKey.asStateFlow()

    private val _openaiApiKey = MutableStateFlow("")
    val openaiApiKey: StateFlow<String> = _openaiApiKey.asStateFlow()

    private val _openaiBaseUrl = MutableStateFlow("https://api.openai.com/")
    val openaiBaseUrl: StateFlow<String> = _openaiBaseUrl.asStateFlow()

    private val _anthropicApiKey = MutableStateFlow("")
    val anthropicApiKey: StateFlow<String> = _anthropicApiKey.asStateFlow()

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _openaiModels = MutableStateFlow<List<String>>(emptyList())
    val openaiModels: StateFlow<List<String>> = _openaiModels.asStateFlow()

    private val _anthropicModels = MutableStateFlow<List<String>>(emptyList())
    val anthropicModels: StateFlow<List<String>> = _anthropicModels.asStateFlow()

    private val _geminiModels = MutableStateFlow<List<String>>(emptyList())
    val geminiModels: StateFlow<List<String>> = _geminiModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    val selectedOpenAIModel = settingsRepository.openAIModel
    val selectedAnthropicModel = settingsRepository.anthropicModel
    val selectedGeminiModel = settingsRepository.geminiModel

    init {
        viewModelScope.launch {
            _selectedProvider.value = aiProviderManager.getActiveProvider()
            _groqKey.value = settingsRepository.customGroqKey.first()
            _openaiApiKey.value = settingsRepository.openAIApiKey.first()
            _openaiBaseUrl.value = settingsRepository.openAIBaseUrl.first()
            _anthropicApiKey.value = settingsRepository.anthropicApiKey.first()
            _geminiApiKey.value = settingsRepository.geminiApiKey.first()
            
            refreshModels()
        }
    }

    fun refreshModels() {
        viewModelScope.launch {
            _isLoadingModels.value = true
            
            // Refresh OpenAI models
            val openaiProvider = aiProviderManager.getProviderService(AIProvider.OPENAI)
            if (openaiProvider != null) {
                _openaiModels.value = openaiProvider.getAvailableModels()
            }

            // Refresh Anthropic models
            val anthropicProvider = aiProviderManager.getProviderService(AIProvider.ANTHROPIC)
            if (anthropicProvider != null) {
                _anthropicModels.value = anthropicProvider.getAvailableModels()
            }

            // Refresh Gemini models
            val geminiProvider = aiProviderManager.getProviderService(AIProvider.GEMINI)
            if (geminiProvider != null) {
                _geminiModels.value = geminiProvider.getAvailableModels()
            }
            
            _isLoadingModels.value = false
        }
    }

    fun selectOpenAIModel(model: String) {
        viewModelScope.launch {
            settingsRepository.saveOpenAIModel(model)
        }
    }

    fun selectAnthropicModel(model: String) {
        viewModelScope.launch {
            settingsRepository.saveAnthropicModel(model)
        }
    }

    fun selectGeminiModel(model: String) {
        viewModelScope.launch {
            settingsRepository.saveGeminiModel(model)
        }
    }

    fun selectProvider(provider: AIProvider) {
        viewModelScope.launch {
            _selectedProvider.value = provider
            aiProviderManager.setActiveProvider(provider)
            settingsRepository.savePreferredAIProvider(provider.name)
        }
    }

    fun saveGroqKey(key: String) {
        viewModelScope.launch {
            _groqKey.value = key
            settingsRepository.saveCustomGroqKey(key)
            if (key.isNotBlank()) {
                settingsRepository.saveUseCustomGroqKey(true)
            }
        }
    }

    fun saveOpenaiKey(key: String) {
        viewModelScope.launch {
            _openaiApiKey.value = key
            settingsRepository.saveOpenAIApiKey(key)
            refreshModels()
        }
    }

    fun saveOpenaiBaseUrl(url: String) {
        viewModelScope.launch {
            _openaiBaseUrl.value = url
            settingsRepository.saveOpenAIBaseUrl(url)
            refreshModels()
        }
    }

    fun saveAnthropicKey(key: String) {
        viewModelScope.launch {
            _anthropicApiKey.value = key
            settingsRepository.saveAnthropicApiKey(key)
            refreshModels()
        }
    }

    fun saveGeminiKey(key: String) {
        viewModelScope.launch {
            _geminiApiKey.value = key
            settingsRepository.saveGeminiApiKey(key)
            refreshModels()
        }
    }
}
