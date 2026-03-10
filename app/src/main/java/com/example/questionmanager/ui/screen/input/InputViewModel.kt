package com.example.questionmanager.ui.screen.input

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questionmanager.data.repository.QuestionRepository
import com.example.questionmanager.domain.model.Prompt
import com.example.questionmanager.domain.model.Question
import com.example.questionmanager.domain.model.QuestionStatus
import com.example.questionmanager.domain.usecase.GenerateAnswerUseCase
import com.example.questionmanager.domain.usecase.ParseContentUseCase
import com.example.questionmanager.domain.usecase.ParseUrlUseCase
import com.example.questionmanager.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InputMode { URL, MANUAL }
data class ParsedQuestion(val text: String, val isSelected: Boolean = true)

@HiltViewModel
class InputViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val parseUrlUseCase: ParseUrlUseCase,
    private val parseContentUseCase: ParseContentUseCase,
    private val generateAnswerUseCase: GenerateAnswerUseCase
) : ViewModel() {

    data class InputUiState(
        val inputMode: InputMode = InputMode.MANUAL,
        // URL 模式
        val url: String = "",
        // 手动模式 — 文本内容输入区
        val manualContent: String = "",
        // 两种模式共用 — AI 解析后的问题列表
        val parsedQuestions: List<ParsedQuestion> = emptyList(),
        val isParsing: Boolean = false,
        val isSubmitting: Boolean = false,
        val selectedPromptId: Long? = null,
        val availablePrompts: List<Prompt> = emptyList(),
        val error: String? = null,
        val isComplete: Boolean = false
    )

    private val _uiState = MutableStateFlow(InputUiState())
    val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    init {
        loadPrompts()
    }

    private fun loadPrompts() {
        viewModelScope.launch {
            questionRepository.getAllPrompts().collect { prompts ->
                val defaultPrompt = prompts.find { it.isDefault }
                _uiState.value = _uiState.value.copy(
                    availablePrompts = prompts,
                    selectedPromptId = _uiState.value.selectedPromptId ?: defaultPrompt?.id
                )
            }
        }
    }

    fun setInputMode(mode: InputMode) {
        _uiState.value = _uiState.value.copy(
            inputMode = mode,
            error = null,
            parsedQuestions = emptyList()
        )
    }

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(url = url)
    }

    fun updateManualContent(content: String) {
        _uiState.value = _uiState.value.copy(manualContent = content)
    }

    /**
     * 解析 URL — 直接发送给 AI，由 AI 根据网址内容总结出问题
     */
    fun parseUrl() {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入网址")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isParsing = true, error = null, parsedQuestions = emptyList()
            )
            parseUrlUseCase(url)
                .onSuccess { questions ->
                    _uiState.value = _uiState.value.copy(
                        isParsing = false,
                        parsedQuestions = questions.map { ParsedQuestion(it) }
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isParsing = false,
                        error = e.message ?: "URL 解析失败"
                    )
                }
        }
    }

    /**
     * 解析手动输入内容 — 发送给 AI，由 AI 整理成结构化问题条目
     */
    fun parseContent() {
        val content = _uiState.value.manualContent.trim()
        if (content.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入内容")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isParsing = true, error = null, parsedQuestions = emptyList()
            )
            parseContentUseCase(content)
                .onSuccess { questions ->
                    _uiState.value = _uiState.value.copy(
                        isParsing = false,
                        parsedQuestions = questions.map { ParsedQuestion(it) }
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isParsing = false,
                        error = e.message ?: "内容解析失败"
                    )
                }
        }
    }

    fun toggleParsedQuestion(index: Int) {
        val current = _uiState.value.parsedQuestions.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(isSelected = !current[index].isSelected)
            _uiState.value = _uiState.value.copy(parsedQuestions = current)
        }
    }

    fun selectPrompt(promptId: Long?) {
        _uiState.value = _uiState.value.copy(selectedPromptId = promptId)
    }

    /**
     * 确认选中的条目并生成答案
     */
    fun confirmAndGenerate() {
        viewModelScope.launch {
            val questionsToCreate = _uiState.value.parsedQuestions
                .filter { it.isSelected }
                .map { it.text }

            if (questionsToCreate.isEmpty()) {
                _uiState.value = _uiState.value.copy(error = "请先解析并选择至少一个问题条目")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                val sourceUrl = if (_uiState.value.inputMode == InputMode.URL) {
                    _uiState.value.url.trim()
                } else null

                val newQuestions = questionsToCreate.map { text ->
                    Question(
                        question = text,
                        status = QuestionStatus.PENDING,
                        promptId = _uiState.value.selectedPromptId,
                        sourceUrl = sourceUrl
                    )
                }

                val ids = questionRepository.insertQuestions(newQuestions)

                // 获取提示词内容
                val promptId = _uiState.value.selectedPromptId
                val selectedPrompt = _uiState.value.availablePrompts.find { it.id == promptId }
                val systemPrompt = selectedPrompt?.systemPrompt ?: Constants.DEFAULT_SYSTEM_PROMPT

                // 异步为每个问题生成答案
                ids.forEach { id ->
                    launch {
                        generateAnswerUseCase(id, systemPrompt)
                    }
                }

                _uiState.value = _uiState.value.copy(isSubmitting = false, isComplete = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "提交失败"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

