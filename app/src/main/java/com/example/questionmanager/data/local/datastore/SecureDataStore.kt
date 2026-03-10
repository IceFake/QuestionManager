package com.example.questionmanager.data.local.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.questionmanager.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        Constants.SECURE_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * 使用 StateFlow 保证 API Key 保存后立即对所有收集者可见
     */
    private val _apiKeyFlow = MutableStateFlow(
        encryptedPrefs.getString(Constants.KEY_API_KEY, "") ?: ""
    )

    fun getApiKey(): Flow<String> = _apiKeyFlow.asStateFlow()

    suspend fun saveApiKey(key: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(Constants.KEY_API_KEY, key).commit()
        _apiKeyFlow.value = key
    }
}

