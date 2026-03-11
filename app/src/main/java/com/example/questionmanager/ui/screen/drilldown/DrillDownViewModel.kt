package com.example.questionmanager.ui.screen.drilldown

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questionmanager.data.repository.QuestionRepository
import com.example.questionmanager.domain.model.Prompt
import com.example.questionmanager.domain.model.Question
import com.example.questionmanager.domain.usecase.BatchGenerateAnswerUseCase
import com.example.questionmanager.domain.usecase.DrillDownUseCase
import com.example.questionmanager.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuggestedQuestion(val text: String, val isSelected: Boolean = true)

@HiltViewModel
class DrillDownViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val drillDownUseCase: DrillDownUseCase,
    private val batchGenerateAnswerUseCase: BatchGenerateAnswerUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val questionId: Long = savedStateHandle["questionId"]!!

    data class DrillDownUiState(
        val originalQuestion: Question? = null,
        val suggestedQuestions: List<SuggestedQuestion> = emptyList(),
        val customQuestions: List<SuggestedQuestion> = emptyList(),
        val isGenerating: Boolean = false,
        val isSubmitting: Boolean = false,
        val isComplete: Boolean = false,
        val selectedPromptId: Long? = null,
        val availablePrompts: List<Prompt> = emptyList(),
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(DrillDownUiState())
    val uiState: StateFlow<DrillDownUiState> = _uiState.asStateFlow()

    init {
        loadOriginalQuestion()
        loadPrompts()
        generateDrillDownQuestions()
    }

    private fun loadOriginalQuestion() {
        viewModelScope.launch {
            try {
                val question = questionRepository.getQuestionById(questionId)
                _uiState.value = _uiState.value.copy(originalQuestion = question)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
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

    fun generateDrillDownQuestions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                error = null,
                suggestedQuestions = emptyList()
            )
            drillDownUseCase.generateSuggestions(questionId)
                .onSuccess { questions ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        suggestedQuestions = questions.map { SuggestedQuestion(it) }
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = e.message ?: "生成引申问题失败"
                    )
                }
        }
    }

    fun toggleQuestion(index: Int) {
        val current = _uiState.value.suggestedQuestions.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(isSelected = !current[index].isSelected)
            _uiState.value = _uiState.value.copy(suggestedQuestions = current)
        }
    }

    fun selectPrompt(promptId: Long?) {
        _uiState.value = _uiState.value.copy(selectedPromptId = promptId)
    }

    fun addCustomQuestion(text: String) {
        if (text.isBlank()) return
        val current = _uiState.value.customQuestions.toMutableList()
        current.add(SuggestedQuestion(text, isSelected = true))
        _uiState.value = _uiState.value.copy(customQuestions = current)
    }

    fun removeCustomQuestion(index: Int) {
        val current = _uiState.value.customQuestions.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.value = _uiState.value.copy(customQuestions = current)
        }
    }

    fun toggleCustomQuestion(index: Int) {
        val current = _uiState.value.customQuestions.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(isSelected = !current[index].isSelected)
            _uiState.value = _uiState.value.copy(customQuestions = current)
        }
    }

    fun confirmSelected() {
        viewModelScope.launch {
            val aiSelected = _uiState.value.suggestedQuestions
                .filter { it.isSelected }
                .map { it.text }

            val customSelected = _uiState.value.customQuestions
                .filter { it.isSelected }
                .map { it.text }

            val allSelected = aiSelected + customSelected

            if (allSelected.isEmpty()) {
                _uiState.value = _uiState.value.copy(error = "请至少选择一个或添加自定义问题")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

            try {
                val newIds = drillDownUseCase.confirmSelected(
                    parentId = questionId,
                    selectedQuestions = allSelected,
                    promptId = _uiState.value.selectedPromptId
                )

                // 获取提示词
                val promptId = _uiState.value.selectedPromptId
                val selectedPrompt = _uiState.value.availablePrompts.find { it.id == promptId }
                val systemPrompt = selectedPrompt?.systemPrompt ?: Constants.DEFAULT_SYSTEM_PROMPT

                // 通过批量用例并行生成答案（每个问题独立对话，独立重试）
                batchGenerateAnswerUseCase.startBatch(
                    questionIds = newIds,
                    systemPrompt = systemPrompt,
                    scope = viewModelScope
                )

                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    isComplete = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "创建条目失败"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

