package com.suvojeet.notenext.data.ai

import com.suvojeet.notenext.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for "is this AI feature allowed to run right now?".
 *
 * Two gates: the master switch (NoteNext's overarching privacy promise — when off,
 * no AI feature ever runs) and the per-feature toggle.
 *
 * UI code should call `isEnabled(feature)` before invoking the corresponding
 * AIProviderManager method, AND should observe `observeIsEnabled(feature)`
 * to hide buttons/affordances for disabled features so the user is never
 * tempted to tap something that wouldn't do anything.
 */
@Singleton
class AIFeatureGate @Inject constructor(
    private val settingsRepository: SettingsRepository
) {

    suspend fun isMasterEnabled(): Boolean = settingsRepository.aiMasterEnabled.first()

    suspend fun isEnabled(feature: AIFeature): Boolean {
        if (!feature.isOnDevice && !settingsRepository.aiMasterEnabled.first()) return false
        return when (feature) {
            AIFeature.SUMMARIZE -> settingsRepository.aiFeatureSummarize.first()
            AIFeature.CHECKLIST -> settingsRepository.aiFeatureChecklist.first()
            AIFeature.TODOS -> settingsRepository.aiFeatureTodos.first()
            AIFeature.GRAMMAR -> settingsRepository.aiFeatureGrammar.first()
            AIFeature.AUTO_TAG -> settingsRepository.aiFeatureAutoTag.first()
            AIFeature.SMART_REMINDER -> settingsRepository.aiFeatureSmartReminder.first()
            AIFeature.LINKED_NOTES -> settingsRepository.aiFeatureLinkedNotes.first()
            AIFeature.TONE_REWRITE -> settingsRepository.aiFeatureToneRewrite.first()
            AIFeature.CUSTOM_PROMPT -> settingsRepository.aiFeatureCustomPrompt.first()
        }
    }

    fun observeIsEnabled(feature: AIFeature): Flow<Boolean> {
        val featureFlow: Flow<Boolean> = when (feature) {
            AIFeature.SUMMARIZE -> settingsRepository.aiFeatureSummarize
            AIFeature.CHECKLIST -> settingsRepository.aiFeatureChecklist
            AIFeature.TODOS -> settingsRepository.aiFeatureTodos
            AIFeature.GRAMMAR -> settingsRepository.aiFeatureGrammar
            AIFeature.AUTO_TAG -> settingsRepository.aiFeatureAutoTag
            AIFeature.SMART_REMINDER -> settingsRepository.aiFeatureSmartReminder
            AIFeature.LINKED_NOTES -> settingsRepository.aiFeatureLinkedNotes
            AIFeature.TONE_REWRITE -> settingsRepository.aiFeatureToneRewrite
            AIFeature.CUSTOM_PROMPT -> settingsRepository.aiFeatureCustomPrompt
        }
        return if (feature.isOnDevice) {
            featureFlow
        } else {
            combine(settingsRepository.aiMasterEnabled, featureFlow) { master, feat ->
                master && feat
            }
        }
    }

    fun observeMasterEnabled(): Flow<Boolean> = settingsRepository.aiMasterEnabled

    /** Map of every feature to its current enabled state — handy for the dashboard. */
    fun observeAllStates(): Flow<Map<AIFeature, Boolean>> = combine(
        settingsRepository.aiMasterEnabled,
        settingsRepository.aiFeatureSummarize,
        settingsRepository.aiFeatureChecklist,
        settingsRepository.aiFeatureTodos,
        settingsRepository.aiFeatureGrammar,
        settingsRepository.aiFeatureAutoTag,
        settingsRepository.aiFeatureSmartReminder,
        settingsRepository.aiFeatureLinkedNotes,
        settingsRepository.aiFeatureToneRewrite,
        settingsRepository.aiFeatureCustomPrompt
    ) { values: Array<Boolean> ->
        val master = values[0]
        AIFeature.values().mapIndexed { i, f -> 
            val featEnabled = values[i + 1]
            f to (if (f.isOnDevice) featEnabled else master && featEnabled)
        }.toMap()
    }
}
