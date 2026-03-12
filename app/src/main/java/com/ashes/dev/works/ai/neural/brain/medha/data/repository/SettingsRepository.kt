package com.ashes.dev.works.ai.neural.brain.medha.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ApiKeyEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "medha_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_API_KEYS = stringPreferencesKey("api_keys_json")
        private val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        private val KEY_APP_MODE = stringPreferencesKey("app_mode")
    }

    val apiKeysFlow: Flow<List<ApiKeyEntry>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_API_KEYS] ?: return@map emptyList()
        deserializeKeys(json)
    }

    val selectedModelFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_MODEL] ?: "gemini-2.0-flash"
    }

    val appModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_MODE] ?: "offline"
    }

    suspend fun saveApiKeys(keys: List<ApiKeyEntry>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_KEYS] = serializeKeys(keys)
        }
    }

    suspend fun saveSelectedModel(modelName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_MODEL] = modelName
        }
    }

    suspend fun saveAppMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_MODE] = mode
        }
    }

    private fun serializeKeys(keys: List<ApiKeyEntry>): String {
        val arr = JSONArray()
        for (entry in keys) {
            arr.put(JSONObject().apply {
                put("id", entry.id)
                put("key", entry.key)
                put("label", entry.label)
                put("isValidated", entry.isValidated)
                put("lastError", entry.lastError ?: "")
                put("addedAt", entry.addedAt)
            })
        }
        return arr.toString()
    }

    private fun deserializeKeys(json: String): List<ApiKeyEntry> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ApiKeyEntry(
                    id = obj.getString("id"),
                    key = obj.getString("key"),
                    label = obj.optString("label", ""),
                    isValidated = obj.optBoolean("isValidated", false),
                    lastError = obj.optString("lastError", "").ifEmpty { null },
                    addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
