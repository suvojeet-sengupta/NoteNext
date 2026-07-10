package com.suvojeet.notenext.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.repository.AiRepository
import com.suvojeet.notenext.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroqSettingsViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val useCustomKey = settingsRepository.useCustomGroqKey
    val customKey = settingsRepository.customGroqKey
    val customFastModel = settingsRepository.customFastModel
    val customLargeModel = settingsRepository.customLargeModel

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels = _availableModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels = _isLoadingModels.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        refreshModels()
    }

    fun refreshModels() {
        viewModelScope.launch {
            _isLoadingModels.value = true
            _error.value = null
            aiRepository.fetchAvailableModels()
                .onSuccess { models ->
                    _availableModels.value = models.map { it.id }.sorted()
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Failed to fetch models"
                }
            _isLoadingModels.value = false
        }
    }

    fun updateUseCustomKey(use: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveUseCustomGroqKey(use)
            if (use) refreshModels()
        }
    }

    fun updateCustomKey(key: String) {
        viewModelScope.launch {
            settingsRepository.saveCustomGroqKey(key)
        }
    }

    fun updateFastModel(model: String) {
        viewModelScope.launch {
            settingsRepository.saveCustomFastModel(model)
        }
    }

    fun updateLargeModel(model: String) {
        viewModelScope.launch {
            settingsRepository.saveCustomLargeModel(model)
        }
    }
}
