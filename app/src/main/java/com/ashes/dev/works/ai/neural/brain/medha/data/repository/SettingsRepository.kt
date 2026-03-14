package com.ashes.dev.works.ai.neural.brain.medha.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ApiKeyEntry
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.CustomGrandMaster
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.Message
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "medha_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_API_KEYS = stringPreferencesKey("api_keys_json")
        private val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        private val KEY_APP_MODE = stringPreferencesKey("app_mode")
        private val KEY_CUSTOM_GRAND_MASTERS = stringPreferencesKey("custom_grand_masters")
        private fun chatHistoryKey(gmKey: String) = stringPreferencesKey("chat_history_$gmKey")
    }

    // ==================== EXISTING SETTINGS ====================

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

    // ==================== CHAT HISTORY ====================

    suspend fun saveChatHistory(gmKey: String, messages: List<Message>) {
        context.dataStore.edit { prefs ->
            prefs[chatHistoryKey(gmKey)] = serializeMessages(messages)
        }
    }

    suspend fun loadChatHistory(gmKey: String): List<Message> {
        val prefs = context.dataStore.data.first()
        val json = prefs[chatHistoryKey(gmKey)] ?: return emptyList()
        return deserializeMessages(json)
    }

    suspend fun clearChatHistory(gmKey: String) {
        context.dataStore.edit { prefs ->
            prefs.remove(chatHistoryKey(gmKey))
        }
    }

    // ==================== CUSTOM GRAND MASTERS ====================

    val customGrandMastersFlow: Flow<List<CustomGrandMaster>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_CUSTOM_GRAND_MASTERS] ?: return@map emptyList()
        deserializeCustomGrandMasters(json)
    }

    suspend fun saveCustomGrandMasters(masters: List<CustomGrandMaster>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_GRAND_MASTERS] = serializeCustomGrandMasters(masters)
        }
    }

    // ==================== SERIALIZATION ====================

    private fun serializeKeys(keys: List<ApiKeyEntry>): String {
        val arr = JSONArray()
        for (entry in keys) {
            arr.put(JSONObject().apply {
                put("id", entry.id)
                put("key", entry.key)
                put("label", entry.label)
                put("baseUrl", entry.baseUrl)
                put("isValidated", entry.isValidated)
                put("isEnabled", entry.isEnabled)
                put("lastError", entry.lastError ?: "")
                put("addedAt", entry.addedAt)
                // Per-key model check results: null value = pass, string = error
                put("checkedModels", JSONObject().apply {
                    entry.checkedModels.forEach { (modelId, error) ->
                        put(modelId, error ?: JSONObject.NULL)
                    }
                })
                put("selectedModels", JSONArray().apply {
                    entry.selectedModels.forEach { put(it) }
                })
            })
        }
        return arr.toString()
    }

    private fun deserializeKeys(json: String): List<ApiKeyEntry> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val checkedModels = mutableMapOf<String, String?>()
                val checkedObj = obj.optJSONObject("checkedModels")
                if (checkedObj != null) {
                    checkedObj.keys().forEach { key ->
                        checkedModels[key] = if (checkedObj.isNull(key)) null else checkedObj.getString(key)
                    }
                }
                val selectedModels = mutableListOf<String>()
                val selectedArr = obj.optJSONArray("selectedModels")
                if (selectedArr != null) {
                    for (j in 0 until selectedArr.length()) {
                        selectedModels.add(selectedArr.getString(j))
                    }
                }
                ApiKeyEntry(
                    id = obj.getString("id"),
                    key = obj.getString("key"),
                    label = obj.optString("label", ""),
                    baseUrl = obj.optString("baseUrl", ""),
                    isValidated = obj.optBoolean("isValidated", false),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    lastError = obj.optString("lastError", "").ifEmpty { null },
                    addedAt = obj.optLong("addedAt", System.currentTimeMillis()),
                    checkedModels = checkedModels,
                    selectedModels = selectedModels
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeMessages(messages: List<Message>): String {
        val arr = JSONArray()
        for (msg in messages) {
            arr.put(JSONObject().apply {
                put("id", msg.id)
                put("text", msg.text)
                put("user", if (msg.user is User.Person) "person" else "ai")
                put("timestamp", msg.timestamp)
                put("imageUri", msg.imageUri ?: "")
            })
        }
        return arr.toString()
    }

    private fun deserializeMessages(json: String): List<Message> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Message(
                    id = obj.getString("id"),
                    text = obj.getString("text"),
                    user = if (obj.getString("user") == "person") User.Person else User.AI,
                    timestamp = obj.getLong("timestamp"),
                    imageUri = obj.optString("imageUri", "").ifEmpty { null }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeCustomGrandMasters(masters: List<CustomGrandMaster>): String {
        val arr = JSONArray()
        for (gm in masters) {
            arr.put(JSONObject().apply {
                put("id", gm.id)
                put("icon", gm.icon)
                put("title", gm.title)
                put("subtitle", gm.subtitle)
                put("description", gm.description)
                put("systemPrompt", gm.systemPrompt)
                put("welcomeMessage", gm.welcomeMessage)
                put("createdAt", gm.createdAt)
            })
        }
        return arr.toString()
    }

    private fun deserializeCustomGrandMasters(json: String): List<CustomGrandMaster> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                CustomGrandMaster(
                    id = obj.getString("id"),
                    icon = obj.optString("icon", "\uD83C\uDF1F"),
                    title = obj.getString("title"),
                    subtitle = obj.optString("subtitle", ""),
                    description = obj.optString("description", ""),
                    systemPrompt = obj.getString("systemPrompt"),
                    welcomeMessage = obj.optString("welcomeMessage", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
