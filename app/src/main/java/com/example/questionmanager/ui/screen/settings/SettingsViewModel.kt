package com.example.questionmanager.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questionmanager.data.repository.QuestionRepository
import com.example.questionmanager.data.repository.SettingsRepository
import com.example.questionmanager.domain.model.Prompt
import com.example.questionmanager.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    data class SettingsUiState(
        val apiKey: String = "",
        val baseUrl: String = Constants.DEFAULT_BASE_URL,
        val model: String = Constants.DEFAULT_MODEL,
        val temperature: Double = Constants.DEFAULT_TEMPERATURE,
        val maxTokens: Int = Constants.DEFAULT_MAX_TOKENS,
        val prompts: List<Prompt> = emptyList(),
        val isSaving: Boolean = false,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadPrompts()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val apiKey = settingsRepository.apiKeyFlow.first()
            val baseUrl = settingsRepository.baseUrlFlow.first()
            val model = settingsRepository.modelFlow.first()
            val temperature = settingsRepository.temperatureFlow.first()
            val maxTokens = settingsRepository.maxTokensFlow.first()
            _uiState.value = _uiState.value.copy(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model,
                temperature = temperature,
                maxTokens = maxTokens
            )
        }
    }

    private fun loadPrompts() {
        viewModelScope.launch {
            questionRepository.getAllPrompts().collect { prompts ->
                _uiState.value = _uiState.value.copy(prompts = prompts)
            }
        }
    }

    fun updateApiKey(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun updateBaseUrl(url: String) {
        _uiState.value = _uiState.value.copy(baseUrl = url)
    }

    fun updateModel(model: String) {
        _uiState.value = _uiState.value.copy(model = model)
    }

    fun updateTemperature(temp: Double) {
        _uiState.value = _uiState.value.copy(temperature = temp)
    }

    fun updateMaxTokens(tokens: Int) {
        _uiState.value = _uiState.value.copy(maxTokens = tokens)
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                settingsRepository.saveApiKey(_uiState.value.apiKey)
                settingsRepository.saveBaseUrl(_uiState.value.baseUrl)
                settingsRepository.saveModel(_uiState.value.model)
                settingsRepository.saveTemperature(_uiState.value.temperature)
                settingsRepository.saveMaxTokens(_uiState.value.maxTokens)
                _uiState.value = _uiState.value.copy(isSaving = false, message = "保存成功")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = "保存失败: ${e.message}"
                )
            }
        }
    }

    fun createPrompt(name: String, content: String) {
        viewModelScope.launch {
            try {
                questionRepository.insertPrompt(
                    Prompt(name = name, systemPrompt = content)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "创建失败: ${e.message}")
            }
        }
    }

    fun updatePrompt(prompt: Prompt) {
        viewModelScope.launch {
            try {
                questionRepository.updatePrompt(
                    prompt.copy(updatedAt = System.currentTimeMillis())
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "更新失败: ${e.message}")
            }
        }
    }

    fun deletePrompt(id: Long) {
        viewModelScope.launch {
            try {
                questionRepository.deletePrompt(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "删除失败: ${e.message}")
            }
        }
    }

    fun setDefaultPrompt(id: Long) {
        viewModelScope.launch {
            try {
                questionRepository.setDefaultPrompt(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "设置失败: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun showMessage(msg: String) {
        _uiState.value = _uiState.value.copy(message = msg)
    }
}

