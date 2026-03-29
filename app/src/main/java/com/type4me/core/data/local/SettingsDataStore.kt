package com.type4me.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.type4me.core.domain.model.ASREngine
import com.type4me.core.domain.model.DefaultPrompts
import com.type4me.core.domain.model.LLMProvider
import com.type4me.core.domain.model.PromptTemplate
import com.type4me.core.domain.model.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "type4me_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val ASR_ENGINE = stringPreferencesKey("asr_engine")
        val PREFER_OFFLINE = booleanPreferencesKey("prefer_offline")
        val AUTO_DOWNLOAD_MODEL = booleanPreferencesKey("auto_download_model")
        val PRIMARY_PROVIDER = stringPreferencesKey("primary_provider")
        val OPENAI_KEY = stringPreferencesKey("openai_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val GEMINI_KEY = stringPreferencesKey("gemini_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val CLAUDE_KEY = stringPreferencesKey("claude_key")
        val CLAUDE_MODEL = stringPreferencesKey("claude_model")
        val AUTO_OPTIMIZE = booleanPreferencesKey("auto_optimize")
        val DEFAULT_PROMPT_ID = stringPreferencesKey("default_prompt_id")
        val CUSTOM_PROMPTS = stringPreferencesKey("custom_prompts")
    }

    val settingsFlow: Flow<Settings> = dataStore.data.map { preferences ->
        Settings(
            asrEngine = preferences[ASR_ENGINE]?.let { ASREngine.valueOf(it) } ?: ASREngine.GOOGLE_ONLINE,
            preferOffline = preferences[PREFER_OFFLINE] ?: false,
            autoDownloadModel = preferences[AUTO_DOWNLOAD_MODEL] ?: true,
            primaryProvider = preferences[PRIMARY_PROVIDER]?.let { LLMProvider.valueOf(it) } ?: LLMProvider.GEMINI_FREE,
            openAiKey = preferences[OPENAI_KEY] ?: "",
            openAiModel = preferences[OPENAI_MODEL] ?: "gpt-3.5-turbo",
            geminiKey = preferences[GEMINI_KEY] ?: "",
            geminiModel = preferences[GEMINI_MODEL] ?: "gemini-pro",
            claudeKey = preferences[CLAUDE_KEY] ?: "",
            claudeModel = preferences[CLAUDE_MODEL] ?: "claude-3-sonnet",
            autoOptimize = preferences[AUTO_OPTIMIZE] ?: false,
            defaultPromptId = preferences[DEFAULT_PROMPT_ID] ?: DefaultPrompts.POLISH.id,
            customPrompts = preferences[CUSTOM_PROMPTS]?.let {
                json.decodeFromString<List<PromptTemplate>>(it)
            } ?: emptyList()
        )
    }

    suspend fun getSettings(): Settings = settingsFlow.first()

    suspend fun updateSettings(settings: Settings) {
        dataStore.edit { preferences ->
            preferences[ASR_ENGINE] = settings.asrEngine.name
            preferences[PREFER_OFFLINE] = settings.preferOffline
            preferences[AUTO_DOWNLOAD_MODEL] = settings.autoDownloadModel
            preferences[PRIMARY_PROVIDER] = settings.primaryProvider.name
            preferences[OPENAI_KEY] = settings.openAiKey
            preferences[OPENAI_MODEL] = settings.openAiModel
            preferences[GEMINI_KEY] = settings.geminiKey
            preferences[GEMINI_MODEL] = settings.geminiModel
            preferences[CLAUDE_KEY] = settings.claudeKey
            preferences[CLAUDE_MODEL] = settings.claudeModel
            preferences[AUTO_OPTIMIZE] = settings.autoOptimize
            preferences[DEFAULT_PROMPT_ID] = settings.defaultPromptId
            preferences[CUSTOM_PROMPTS] = json.encodeToString(settings.customPrompts)
        }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }
}
