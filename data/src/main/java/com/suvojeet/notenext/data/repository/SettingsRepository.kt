package com.suvojeet.notenext.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.suvojeet.notenext.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import androidx.datastore.preferences.SharedPreferencesMigration

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "ai_prompt_history_prefs"))
    }
)

object PreferencesKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
    val ENABLE_RICH_LINK_PREVIEW = booleanPreferencesKey("enable_rich_link_preview")
    val ENABLE_APP_LOCK = booleanPreferencesKey("enable_app_lock")
    val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
    val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
    val LANGUAGE = stringPreferencesKey("language")
    val LAST_SEEN_VERSION = intPreferencesKey("last_seen_version")
    val DISALLOW_SCREENSHOTS = booleanPreferencesKey("disallow_screenshots")
    val CLIPBOARD_CLEAR_TIMEOUT = longPreferencesKey("clipboard_clear_timeout")
    val ENABLE_DECOY_VAULT = booleanPreferencesKey("enable_decoy_vault")
    val DECOY_PIN = stringPreferencesKey("decoy_pin")

    // Groq API Settings
    val USE_CUSTOM_GROQ_KEY = booleanPreferencesKey("use_custom_groq_key")
    val CUSTOM_GROQ_KEY = stringPreferencesKey("custom_groq_key")
    val CUSTOM_FAST_MODEL = stringPreferencesKey("custom_fast_model")
    val CUSTOM_LARGE_MODEL = stringPreferencesKey("custom_large_model")
    
    // AI Provider Settings
    val PREFERRED_AI_PROVIDER = stringPreferencesKey("preferred_ai_provider")
    val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
    val OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
    val OPENAI_MODEL = stringPreferencesKey("openai_model")
    val ANTHROPIC_API_KEY = stringPreferencesKey("anthropic_api_key")
    val ANTHROPIC_MODEL = stringPreferencesKey("anthropic_model")
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    val GEMINI_MODEL = stringPreferencesKey("gemini_model")

    // AI Feature Toggles (privacy-first: ALL default OFF, master switch off)
    val AI_MASTER_ENABLED = booleanPreferencesKey("ai_master_enabled")
    val AI_USAGE_TRACKING_ENABLED = booleanPreferencesKey("ai_usage_tracking_enabled")
    val AI_FEATURE_SUMMARIZE = booleanPreferencesKey("ai_feature_summarize")
    val AI_FEATURE_CHECKLIST = booleanPreferencesKey("ai_feature_checklist")
    val AI_FEATURE_TODOS = booleanPreferencesKey("ai_feature_todos")
    val AI_FEATURE_GRAMMAR = booleanPreferencesKey("ai_feature_grammar")
    val AI_FEATURE_AUTO_TAG = booleanPreferencesKey("ai_feature_auto_tag")
    val AI_FEATURE_SMART_REMINDER = booleanPreferencesKey("ai_feature_smart_reminder")
    val AI_FEATURE_LINKED_NOTES = booleanPreferencesKey("ai_feature_linked_notes")
    val AI_FEATURE_TONE_REWRITE = booleanPreferencesKey("ai_feature_tone_rewrite")
    val AI_FEATURE_CUSTOM_PROMPT = booleanPreferencesKey("ai_feature_custom_prompt")
    val AI_PROMPT_HISTORY = stringPreferencesKey("ai_prompt_history")
}

class SettingsRepository(private val context: Context) {

    val aiPromptHistory: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val historyString = preferences[PreferencesKeys.AI_PROMPT_HISTORY] ?: ""
            if (historyString.isBlank()) emptyList() else historyString.split("|||")
        }

    suspend fun saveAiPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            val currentHistory = (preferences[PreferencesKeys.AI_PROMPT_HISTORY] ?: "").split("|||").filter { it.isNotBlank() }
            val newHistory = (listOf(prompt) + currentHistory.filter { it != prompt }).take(10)
            preferences[PreferencesKeys.AI_PROMPT_HISTORY] = newHistory.joinToString("|||")
        }
    }
    
    // Groq API Settings
    val useCustomGroqKey: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.USE_CUSTOM_GROQ_KEY] ?: false }

    suspend fun saveUseCustomGroqKey(use: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.USE_CUSTOM_GROQ_KEY] = use }
    }

    val customGroqKey: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.CUSTOM_GROQ_KEY] ?: "" }

    suspend fun saveCustomGroqKey(key: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.CUSTOM_GROQ_KEY] = key }
    }

    val customFastModel: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.CUSTOM_FAST_MODEL] ?: "llama-3.1-8b-instant" }

    suspend fun saveCustomFastModel(model: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.CUSTOM_FAST_MODEL] = model }
    }

    val customLargeModel: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.CUSTOM_LARGE_MODEL] ?: "llama-3.3-70b-versatile" }

    suspend fun saveCustomLargeModel(model: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.CUSTOM_LARGE_MODEL] = model }
    }

    val lastSeenVersion: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_SEEN_VERSION] ?: 0
        }

    suspend fun saveLastSeenVersion(version: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SEEN_VERSION] = version
        }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map {
            preferences ->
            ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    val autoDeleteDays: Flow<Int> = context.dataStore.data
        .map {
            preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] ?: 7
        }

    suspend fun saveAutoDeleteDays(days: Int) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] = days
        }
    }

    val enableRichLinkPreview: Flow<Boolean> = context.dataStore.data
        .map {
            preferences ->
            preferences[PreferencesKeys.ENABLE_RICH_LINK_PREVIEW] ?: true
        }

    suspend fun saveEnableRichLinkPreview(enable: Boolean) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.ENABLE_RICH_LINK_PREVIEW] = enable
        }
    }

    val enableAppLock: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ENABLE_APP_LOCK] ?: false
        }

    suspend fun saveEnableAppLock(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_APP_LOCK] = enable
            // Disabling app lock also drops the stored PIN AND the decoy PIN. Without
            // this, turning lock off and back on later silently restores the old PIN —
            // surprising behavior and a forensic-trail concern. Decoy is meaningless
            // without the parent app lock, so it goes too.
            if (!enable) {
                preferences.remove(PreferencesKeys.APP_LOCK_PIN)
                preferences.remove(PreferencesKeys.DECOY_PIN)
                preferences[PreferencesKeys.ENABLE_DECOY_VAULT] = false
            }
        }
    }

    // Stored value is `salt:hash` (PBKDF2-HMAC-SHA256, 100k iterations).
    // We never expose the stored hash — only a "is set" flag and a `verify` helper.
    val isAppLockPinSet: Flow<Boolean> = context.dataStore.data
        .map { preferences -> !preferences[PreferencesKeys.APP_LOCK_PIN].isNullOrEmpty() }

    suspend fun saveAppLockPin(pin: String) {
        val hashed = PinHasher.hash(pin)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_PIN] = hashed
        }
    }

    suspend fun clearAppLockPin() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.APP_LOCK_PIN)
        }
    }

    suspend fun verifyAppLockPin(input: String): Boolean {
        val stored = context.dataStore.data.first()[PreferencesKeys.APP_LOCK_PIN] ?: return false
        val ok = PinHasher.verify(input, stored)
        // Lazy migration: if the user is verifying against a legacy plaintext PIN
        // (saved by a pre-hash build), re-save it hashed now that we know the PIN is
        // correct. After the next successful unlock, no plaintext PIN remains in prefs.
        if (ok && PinHasher.isLegacyPlaintext(stored)) {
            saveAppLockPin(input)
        }
        return ok
    }

    val isSetupComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] ?: false
        }

    suspend fun setSetupComplete(isComplete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] = isComplete
        }
    }

    val language: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LANGUAGE] ?: "en"
        }

    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    val disallowScreenshots: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DISALLOW_SCREENSHOTS] ?: false
        }

    suspend fun saveDisallowScreenshots(disallow: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISALLOW_SCREENSHOTS] = disallow
        }
    }

    val clipboardClearTimeout: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CLIPBOARD_CLEAR_TIMEOUT] ?: 0L
        }

    suspend fun saveClipboardClearTimeout(timeout: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.CLIPBOARD_CLEAR_TIMEOUT] = timeout }
    }

    val enableDecoyVault: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.ENABLE_DECOY_VAULT] ?: false }

    suspend fun saveEnableDecoyVault(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_DECOY_VAULT] = enable
            // Disabling the feature must also drop the stored decoy PIN — otherwise
            // turning the feature off and on later silently re-enables the old PIN,
            // and a forensic dump of prefs reveals the feature was ever configured.
            if (!enable) {
                preferences.remove(PreferencesKeys.DECOY_PIN)
            }
        }
    }

    val isDecoyPinSet: Flow<Boolean> = context.dataStore.data
        .map { preferences -> !preferences[PreferencesKeys.DECOY_PIN].isNullOrEmpty() }

    suspend fun saveDecoyPin(pin: String) {
        val hashed = PinHasher.hash(pin)
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DECOY_PIN] = hashed }
    }

    suspend fun clearDecoyPin() {
        context.dataStore.edit { preferences -> preferences.remove(PreferencesKeys.DECOY_PIN) }
    }

    suspend fun verifyDecoyPin(input: String): Boolean {
        val stored = context.dataStore.data.first()[PreferencesKeys.DECOY_PIN] ?: return false
        val ok = PinHasher.verify(input, stored)
        if (ok && PinHasher.isLegacyPlaintext(stored)) {
            saveDecoyPin(input)
        }
        return ok
    }

    /**
     * True if the input would match the *real* PIN if it were saved as the decoy PIN.
     * Used by the settings dialog to refuse a decoy PIN equal to the app lock PIN.
     */
    suspend fun decoyPinClashesWithReal(candidate: String): Boolean = verifyAppLockPin(candidate)


    // AI Provider Settings
    val preferredAIProvider: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.PREFERRED_AI_PROVIDER] ?: "GROQ" }

    suspend fun savePreferredAIProvider(provider: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.PREFERRED_AI_PROVIDER] = provider }
    }

    val openAIApiKey: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.OPENAI_API_KEY] ?: "" }

    suspend fun saveOpenAIApiKey(key: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.OPENAI_API_KEY] = key }
    }

    val openAIBaseUrl: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.OPENAI_BASE_URL] ?: "https://api.openai.com/" }

    suspend fun saveOpenAIBaseUrl(url: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.OPENAI_BASE_URL] = url }
    }

    val openAIModel: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.OPENAI_MODEL] ?: "gpt-4o-mini" }

    suspend fun saveOpenAIModel(model: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.OPENAI_MODEL] = model }
    }

    val anthropicApiKey: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.ANTHROPIC_API_KEY] ?: "" }

    suspend fun saveAnthropicApiKey(key: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.ANTHROPIC_API_KEY] = key }
    }

    val anthropicModel: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.ANTHROPIC_MODEL] ?: "claude-3-5-sonnet-20241022" }

    suspend fun saveAnthropicModel(model: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.ANTHROPIC_MODEL] = model }
    }

    val geminiApiKey: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.GEMINI_API_KEY] ?: "" }

    suspend fun saveGeminiApiKey(key: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.GEMINI_API_KEY] = key }
    }

    val geminiModel: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.GEMINI_MODEL] ?: "gemini-3.1-flash" }

    suspend fun saveGeminiModel(model: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.GEMINI_MODEL] = model }
    }

    // ─── AI Feature Toggles (privacy-first, default OFF) ─────────────────
    // The master switch must be ON before any feature toggle has effect.
    // This is the core of NoteNext's privacy promise: nothing leaves the
    // device until the user explicitly opts in.

    val aiMasterEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_MASTER_ENABLED] ?: false }

    suspend fun saveAiMasterEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_MASTER_ENABLED] = enabled }
    }

    val aiUsageTrackingEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_USAGE_TRACKING_ENABLED] ?: true }

    suspend fun saveAiUsageTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_USAGE_TRACKING_ENABLED] = enabled }
    }

    val aiFeatureSummarize: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_FEATURE_SUMMARIZE] ?: false }
    suspend fun saveAiFeatureSummarize(v: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_FEATURE_SUMMARIZE] = v }
    }

    val aiFeatureChecklist: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_FEATURE_CHECKLIST] ?: false }
    suspend fun saveAiFeatureChecklist(v: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_FEATURE_CHECKLIST] = v }
    }

    val aiFeatureTodos: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_FEATURE_TODOS] ?: false }
    suspend fun saveAiFeatureTodos(v: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_FEATURE_TODOS] = v }
    }

    val aiFeatureGrammar: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_FEATURE_GRAMMAR] ?: false }
    suspend fun saveAiFeatureGrammar(v: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_FEATURE_GRAMMAR] = v }
    }

    val aiFeatureAutoTag: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_FEATURE_AUTO_TAG] ?: false }
    suspend fun saveAiFeatureAutoTag(v: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_FEATURE_AUTO_TAG] = v }
    }

    val aiFeatureSmartReminder: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_FEATURE_SMART_REMINDER] ?: false }
    suspend fun saveAiFeatureSmartReminder(v: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_FEATURE_SMART_REMINDER] = v }
    }

    val aiFeatureLinkedNotes: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_FEATURE_LINKED_NOTES] ?: false }
    suspend fun saveAiFeatureLinkedNotes(v: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_FEATURE_LINKED_NOTES] = v }
    }

    val aiFeatureToneRewrite: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_FEATURE_TONE_REWRITE] ?: false }
    suspend fun saveAiFeatureToneRewrite(v: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_FEATURE_TONE_REWRITE] = v }
    }

    val aiFeatureCustomPrompt: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.AI_FEATURE_CUSTOM_PROMPT] ?: false }
    suspend fun saveAiFeatureCustomPrompt(v: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_FEATURE_CUSTOM_PROMPT] = v }
    }
}

/**
 * Salted PBKDF2-HMAC-SHA256 PIN hashing.
 *
 * 4-digit PINs are inherently low-entropy — a Keystore-bound HMAC would be ideal, but
 * adds significant complexity. Salted PBKDF2 at 100k iterations turns a stolen prefs
 * file into a real (if not insurmountable) computational cost: roughly 100k * 10000 =
 * 1 billion HMAC ops to brute-force a 4-digit PIN, vs. zero for the previous plaintext
 * storage. Combined with `data_extraction_rules.xml` excluding sharedpref from cloud
 * backup, casual exfiltration is no longer enough.
 *
 * Stored format: "salt_b64:hash_b64". Backwards-compatible reads handled by `verify`.
 */
private object PinHasher {
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val ALGO = "PBKDF2WithHmacSHA256"

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val derived = pbkdf2(pin, salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(derived, Base64.NO_WRAP)
    }

    fun verify(input: String, stored: String): Boolean {
        if (isLegacyPlaintext(stored)) {
            // Legacy plaintext PIN (pre-hash migration). Constant-time compare.
            return constantTimeEquals(input.toByteArray(Charsets.UTF_8), stored.toByteArray(Charsets.UTF_8))
        }
        val parts = stored.split(":")
        return try {
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expected = Base64.decode(parts[1], Base64.NO_WRAP)
            val actual = pbkdf2(input, salt)
            constantTimeEquals(expected, actual)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * A plaintext PIN from before the hashing migration. Detected by the absence of
     * the `salt:hash` separator. Verifying such a value succeeds, after which the
     * caller should re-save the PIN through `saveAppLockPin`/`saveDecoyPin` so the
     * plaintext is replaced on disk.
     */
    fun isLegacyPlaintext(stored: String): Boolean = !stored.contains(":")

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance(ALGO).generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        return MessageDigest.isEqual(a, b)
    }
}