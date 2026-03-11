package com.example.questionmanager.ui.screen.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questionmanager.data.remote.api.DeepSeekApiService
import com.example.questionmanager.data.remote.model.DeepSeekRequest
import com.example.questionmanager.data.remote.model.Message
import com.example.questionmanager.data.repository.QuestionRepository
import com.example.questionmanager.data.repository.SettingsRepository
import com.example.questionmanager.di.DynamicBaseUrl
import com.example.questionmanager.domain.model.Prompt
import com.example.questionmanager.util.Constants
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val questionRepository: QuestionRepository,
    private val deepSeekApiService: DeepSeekApiService
) : ViewModel() {

    data class SettingsUiState(
        val apiKey: String = "",
        val baseUrl: String = Constants.DEFAULT_BASE_URL,
        val model: String = Constants.DEFAULT_MODEL,
        val temperature: Double = Constants.DEFAULT_TEMPERATURE,
        val maxTokens: Int = Constants.DEFAULT_MAX_TOKENS,
        val prompts: List<Prompt> = emptyList(),
        val isSaving: Boolean = false,
        val isEnhancingPrompt: Boolean = false,
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

    /**
     * 从 PDF 简历中提取文本
     */
    private suspend fun extractPdfText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("无法打开 PDF 文件")
        inputStream.use { stream ->
            val document = PDDocument.load(stream)
            document.use { doc ->
                val stripper = PDFTextStripper()
                stripper.getText(doc)
            }
        }
    }

    /**
     * 导入 PDF 简历并调用 AI 接口完善提示词
     * @param context Android Context，用于读取文件
     * @param uri     选中的 PDF 文件 URI
     * @param currentPrompt 当前已有的提示词内容（可为空）
     * @param onResult 回调：将 AI 生成的完善后提示词返回给 UI
     */
    fun importPdfAndEnhancePrompt(
        context: Context,
        uri: Uri,
        currentPrompt: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEnhancingPrompt = true)
            try {
                // 1. 提取 PDF 文本
                val resumeText = extractPdfText(context, uri)
                if (resumeText.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isEnhancingPrompt = false,
                        message = "PDF 内容为空，无法提取文本"
                    )
                    return@launch
                }

                // 2. 同步 Base URL
                val baseUrl = settingsRepository.baseUrlFlow.first()
                if (baseUrl.isNotBlank()) {
                    DynamicBaseUrl.baseUrl = baseUrl
                }

                // 3. 调用 AI 接口完善提示词
                val apiKey = settingsRepository.apiKeyFlow.first()
                if (apiKey.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isEnhancingPrompt = false,
                        message = "请先配置 API Key"
                    )
                    return@launch
                }

                val model = settingsRepository.modelFlow.first()

                val systemMessage = """你是一个专业的提示词工程师。用户将提供一份 PDF 简历的文本内容，以及一段可选的现有提示词。
请根据简历中的专业背景、技能、经验等信息，生成或完善一段高质量的 System Prompt。

要求：
1. 提示词应充分利用简历中的专业领域和技能特长，让 AI 助手能以该领域专家的身份回答问题
2. 提示词要具体、有针对性，不要泛泛而谈
3. 如果已有提示词内容，请在其基础上融入简历信息进行增强，保留原有合理部分
4. 直接输出最终的 System Prompt 内容，不要添加解释说明
5. 使用中文输出"""

                val userMessage = buildString {
                    if (currentPrompt.isNotBlank()) {
                        append("现有提示词：\n$currentPrompt\n\n")
                    }
                    append("简历内容：\n$resumeText")
                }

                val request = DeepSeekRequest(
                    model = model,
                    messages = listOf(
                        Message(role = "system", content = systemMessage),
                        Message(role = "user", content = userMessage)
                    ),
                    temperature = 0.7,
                    maxTokens = 2048
                )

                val response = deepSeekApiService.chatCompletion(
                    authHeader = "Bearer $apiKey",
                    request = request
                )

                val enhancedPrompt = response.choices.firstOrNull()?.message?.content
                if (enhancedPrompt.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isEnhancingPrompt = false,
                        message = "AI 返回内容为空"
                    )
                    return@launch
                }

                onResult(enhancedPrompt)
                _uiState.value = _uiState.value.copy(
                    isEnhancingPrompt = false,
                    message = "已根据简历内容完善提示词"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isEnhancingPrompt = false,
                    message = "导入简历失败: ${e.message}"
                )
            }
        }
    }
}

