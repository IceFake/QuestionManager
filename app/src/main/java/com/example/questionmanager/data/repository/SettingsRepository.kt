package com.example.questionmanager.data.repository

import com.example.questionmanager.data.local.datastore.SecureDataStore
import com.example.questionmanager.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val secureDataStore: SecureDataStore
) {
    // API Key (加密存储)
    val apiKeyFlow: Flow<String> = secureDataStore.getApiKey()
    suspend fun saveApiKey(key: String) = secureDataStore.saveApiKey(key)

    // Base URL
    val baseUrlFlow: Flow<String> = settingsDataStore.baseUrlFlow
    suspend fun saveBaseUrl(url: String) = settingsDataStore.saveBaseUrl(url)

    // 模型名
    val modelFlow: Flow<String> = settingsDataStore.modelFlow
    suspend fun saveModel(model: String) = settingsDataStore.saveModel(model)

    // Temperature
    val temperatureFlow: Flow<Double> = settingsDataStore.temperatureFlow
    suspend fun saveTemperature(temp: Double) = settingsDataStore.saveTemperature(temp)

    // Max Tokens
    val maxTokensFlow: Flow<Int> = settingsDataStore.maxTokensFlow
    suspend fun saveMaxTokens(tokens: Int) = settingsDataStore.saveMaxTokens(tokens)
}

