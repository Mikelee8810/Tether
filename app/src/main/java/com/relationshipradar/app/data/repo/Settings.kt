package com.relationshipradar.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("settings")

data class AppSettings(
    val roundupHour: Int = 9,
    val roundupEnabled: Boolean = true,
    val individualAlertsEnabled: Boolean = true,
    val lastContactsSyncAt: Long = 0L,
    val onboardingDone: Boolean = false,
    val lastReminderRunAt: Long = 0L,
    val lastScanRunAt: Long = 0L,
    val aiProvider: String = "gemini", // "gemini", "groq", "openrouter", "grok", "openai", "claude", "custom"
    val geminiApiKey: String = "",
    val groqApiKey: String = "",
    val openRouterApiKey: String = "",
    val grokApiKey: String = "",
    val openAiApiKey: String = "",
    val anthropicApiKey: String = "",
    val customApiBaseUrl: String = "",
    val customApiKey: String = "",
    val customModelName: String = "",
    val orbitSparks: Int = 0,
    val weeklyConnectionsCount: Int = 0,
    val lastConnectionDate: Long = 0L,
)

class Settings(private val context: Context) {
    private object K {
        val roundupHour = intPreferencesKey("roundup_hour")
        val roundupEnabled = booleanPreferencesKey("roundup_enabled")
        val individual = booleanPreferencesKey("individual_alerts")
        val lastSync = longPreferencesKey("last_contacts_sync")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val lastReminderRun = longPreferencesKey("last_reminder_run")
        val lastScanRun = longPreferencesKey("last_scan_run")
        val aiProvider = stringPreferencesKey("ai_provider")
        val geminiApiKey = stringPreferencesKey("gemini_api_key")
        val groqApiKey = stringPreferencesKey("groq_api_key")
        val openRouterApiKey = stringPreferencesKey("openrouter_api_key")
        val grokApiKey = stringPreferencesKey("grok_api_key")
        val openAiApiKey = stringPreferencesKey("openai_api_key")
        val anthropicApiKey = stringPreferencesKey("anthropic_api_key")
        val customApiBaseUrl = stringPreferencesKey("custom_api_base_url")
        val customApiKey = stringPreferencesKey("custom_api_key")
        val customModelName = stringPreferencesKey("custom_model_name")
        val orbitSparks = intPreferencesKey("orbit_sparks")
        val weeklyConnectionsCount = intPreferencesKey("weekly_connections_count")
        val lastConnectionDate = longPreferencesKey("last_connection_date")
    }

    val flow: Flow<AppSettings> = context.store.data.map { p ->
        AppSettings(
            roundupHour = p[K.roundupHour] ?: 9,
            roundupEnabled = p[K.roundupEnabled] ?: true,
            individualAlertsEnabled = p[K.individual] ?: true,
            lastContactsSyncAt = p[K.lastSync] ?: 0L,
            onboardingDone = p[K.onboarding] ?: false,
            lastReminderRunAt = p[K.lastReminderRun] ?: 0L,
            lastScanRunAt = p[K.lastScanRun] ?: 0L,
            aiProvider = p[K.aiProvider] ?: "gemini",
            geminiApiKey = p[K.geminiApiKey] ?: "",
            groqApiKey = p[K.groqApiKey] ?: "",
            openRouterApiKey = p[K.openRouterApiKey] ?: "",
            grokApiKey = p[K.grokApiKey] ?: "",
            openAiApiKey = p[K.openAiApiKey] ?: "",
            anthropicApiKey = p[K.anthropicApiKey] ?: "",
            customApiBaseUrl = p[K.customApiBaseUrl] ?: "",
            customApiKey = p[K.customApiKey] ?: "",
            customModelName = p[K.customModelName] ?: "",
            orbitSparks = p[K.orbitSparks] ?: 0,
            weeklyConnectionsCount = p[K.weeklyConnectionsCount] ?: 0,
            lastConnectionDate = p[K.lastConnectionDate] ?: 0L,
        )
    }

    suspend fun setRoundupHour(h: Int) = context.store.edit { it[K.roundupHour] = h }
    suspend fun setRoundupEnabled(v: Boolean) = context.store.edit { it[K.roundupEnabled] = v }
    suspend fun setIndividualAlerts(v: Boolean) = context.store.edit { it[K.individual] = v }
    suspend fun setLastSync(t: Long) = context.store.edit { it[K.lastSync] = t }
    suspend fun markReminderRun(t: Long) = context.store.edit { it[K.lastReminderRun] = t }
    suspend fun markScanRun(t: Long) = context.store.edit { it[K.lastScanRun] = t }
    suspend fun setOnboardingDone() = context.store.edit { it[K.onboarding] = true }
    suspend fun setAiProvider(provider: String) = context.store.edit { it[K.aiProvider] = provider }
    suspend fun setGeminiApiKey(key: String) = context.store.edit { it[K.geminiApiKey] = key.trim() }
    suspend fun setGroqApiKey(key: String) = context.store.edit { it[K.groqApiKey] = key.trim() }
    suspend fun setOpenRouterApiKey(key: String) = context.store.edit { it[K.openRouterApiKey] = key.trim() }
    suspend fun setGrokApiKey(key: String) = context.store.edit { it[K.grokApiKey] = key.trim() }
    suspend fun setOpenAiApiKey(key: String) = context.store.edit { it[K.openAiApiKey] = key.trim() }
    suspend fun setAnthropicApiKey(key: String) = context.store.edit { it[K.anthropicApiKey] = key.trim() }
    suspend fun setCustomApi(baseUrl: String, key: String, model: String) = context.store.edit {
        it[K.customApiBaseUrl] = baseUrl.trim()
        it[K.customApiKey] = key.trim()
        it[K.customModelName] = model.trim()
    }
    suspend fun addSparks(amount: Int) = context.store.edit {
        val current = it[K.orbitSparks] ?: 0
        it[K.orbitSparks] = current + amount
    }
    suspend fun recordCatchUpGamification(sparks: Int = 10) = context.store.edit {
        val currentSparks = it[K.orbitSparks] ?: 0
        val currentCount = it[K.weeklyConnectionsCount] ?: 0
        it[K.orbitSparks] = currentSparks + sparks
        it[K.weeklyConnectionsCount] = currentCount + 1
        it[K.lastConnectionDate] = System.currentTimeMillis()
    }
}
