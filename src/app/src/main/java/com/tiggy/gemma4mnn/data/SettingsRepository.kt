package com.tiggy.gemma4mnn.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for user preferences using DataStore.
 */
class SettingsRepository(context: Context) {
    private val dataStore = context.dataStore

    // Keys
    private val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
    private val KEY_THINKING_ENABLED = booleanPreferencesKey("thinking_enabled")
    private val KEY_CONTEXT_LENGTH = intPreferencesKey("context_length")
    private val KEY_TEMPERATURE = floatPreferencesKey("temperature")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

    // Flows
    val selectedModel: Flow<String?> = dataStore.data.map { it[KEY_SELECTED_MODEL] }
    val thinkingEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_THINKING_ENABLED] ?: false }
    val contextLength: Flow<Int> = dataStore.data.map { it[KEY_CONTEXT_LENGTH] ?: 4096 }
    val temperature: Flow<Float> = dataStore.data.map { it[KEY_TEMPERATURE] ?: 0.7f }
    val themeMode: Flow<String> = dataStore.data.map { it[KEY_THEME_MODE] ?: "system" }

    suspend fun setSelectedModel(modelName: String) {
        dataStore.edit { it[KEY_SELECTED_MODEL] = modelName }
    }

    suspend fun setThinkingEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_THINKING_ENABLED] = enabled }
    }

    suspend fun setContextLength(length: Int) {
        dataStore.edit { it[KEY_CONTEXT_LENGTH] = length }
    }

    suspend fun setTemperature(temp: Float) {
        dataStore.edit { it[KEY_TEMPERATURE] = temp }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }
}
