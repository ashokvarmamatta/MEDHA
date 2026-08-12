package com.ashes.dev.works.ai.neural.brain.medha.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "medha_settings")

/**
 * App preferences that outlive a chat session. Conversations themselves live in
 * [com.ashes.dev.works.ai.neural.brain.medha.data.local.ChatDatabase], so all this
 * has to remember is which on-device model to bring back up on launch.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
    }

    /** File name of the on-device model the user last ran; blank on a fresh install. */
    val selectedModelFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_MODEL] ?: ""
    }

    suspend fun saveSelectedModel(modelName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_MODEL] = modelName
        }
    }
}
