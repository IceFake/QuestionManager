package com.example.questionmanager.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.questionmanager.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.SETTINGS_DATASTORE_NAME
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val BASE_URL = stringPreferencesKey(Constants.KEY_BASE_URL)
        val MODEL = stringPreferencesKey(Constants.KEY_MODEL)
        val TEMPERATURE = doublePreferencesKey(Constants.KEY_TEMPERATURE)
        val MAX_TOKENS = intPreferencesKey(Constants.KEY_MAX_TOKENS)
    }

    val baseUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.BASE_URL] ?: Constants.DEFAULT_BASE_URL
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = url
        }
    }

    val modelFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.MODEL] ?: Constants.DEFAULT_MODEL
    }

    suspend fun saveModel(model: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MODEL] = model
        }
    }

    val temperatureFlow: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[Keys.TEMPERATURE] ?: Constants.DEFAULT_TEMPERATURE
    }

    suspend fun saveTemperature(temp: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEMPERATURE] = temp
        }
    }

    val maxTokensFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.MAX_TOKENS] ?: Constants.DEFAULT_MAX_TOKENS
    }

    suspend fun saveMaxTokens(tokens: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_TOKENS] = tokens
        }
    }
}

