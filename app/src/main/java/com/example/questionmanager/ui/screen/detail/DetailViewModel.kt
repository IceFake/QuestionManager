package com.example.questionmanager.ui.screen.detail

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questionmanager.data.repository.QuestionRepository
import com.example.questionmanager.domain.model.Prompt
import com.example.questionmanager.domain.model.Question
import com.example.questionmanager.domain.usecase.GenerateAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val generateAnswerUseCase: GenerateAnswerUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val questionId: Long = savedStateHandle["questionId"]!!

    data class DetailUiState(
        val question: Question? = null,
        val parentQuestions: List<Question> = emptyList(),
        val childQuestions: List<Question> = emptyList(),
        val prompts: List<Prompt> = emptyList(),
        val selectedPromptId: Long? = null,
        val isRegenerating: Boolean = false,
        val streamingAnswer: String? = null,
        val isDeleted: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadQuestion()
        loadParentQuestions()
        loadChildQuestions()
        loadPrompts()
    }

    private fun loadQuestion() {
        viewModelScope.launch {
            try {
                val question = questionRepository.getQuestionById(questionId)
                _uiState.value = _uiState.value.copy(
                    question = question,
                    selectedPromptId = question?.promptId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun refreshQuestion() {
        loadQuestion()
    }

    private fun loadPrompts() {
        viewModelScope.launch {
            questionRepository.getAllPrompts()
                .catch { }
                .collect { prompts ->
                    _uiState.value = _uiState.value.copy(
                        prompts = prompts,
                        selectedPromptId = _uiState.value.selectedPromptId ?: prompts.find { it.isDefault }?.id
                    )
                }
        }
    }

    fun selectPrompt(promptId: Long?) {
        _uiState.value = _uiState.value.copy(selectedPromptId = promptId)
    }

    private fun loadParentQuestions() {
        viewModelScope.launch {
            questionRepository.getParentQuestions(questionId)
                .catch { /* ignore */ }
                .collect { parents ->
                    _uiState.value = _uiState.value.copy(parentQuestions = parents)
                }
        }
    }

    private fun loadChildQuestions() {
        viewModelScope.launch {
            questionRepository.getChildQuestions(questionId)
                .catch { /* ignore */ }
                .collect { children ->
                    _uiState.value = _uiState.value.copy(childQuestions = children)
                }
        }
    }

    /**
     * 流式重新生成答案 — 答案逐步显示
     */
    fun regenerateAnswer() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRegenerating = true,
                streamingAnswer = "",
                error = null
            )
            try {
                val promptId = _uiState.value.selectedPromptId
                val systemPrompt = promptId?.let { questionRepository.getPromptById(it)?.systemPrompt }

                generateAnswerUseCase.stream(questionId, systemPrompt)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isRegenerating = false,
                            streamingAnswer = null,
                            error = "生成失败: ${e.message}"
                        )
                        loadQuestion()
                    }
                    .collect { partial ->
                        _uiState.value = _uiState.value.copy(streamingAnswer = partial)
                    }
                // Stream completed — reload from DB
                val updated = questionRepository.getQuestionById(questionId)
                _uiState.value = _uiState.value.copy(
                    question = updated,
                    isRegenerating = false,
                    streamingAnswer = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRegenerating = false,
                    streamingAnswer = null,
                    error = "生成失败: ${e.message}"
                )
                loadQuestion()
            }
        }
    }

    fun deleteQuestion() {
        viewModelScope.launch {
            try {
                questionRepository.deleteQuestion(questionId)
                _uiState.value = _uiState.value.copy(isDeleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "删除失败: ${e.message}")
            }
        }
    }

    /**
     * 分享/导出问题和答案
     */
    fun shareQuestion(context: Context) {
        val q = _uiState.value.question ?: return
        val text = buildString {
            appendLine("Q: ${q.question}")
            appendLine()
            if (!q.answer.isNullOrBlank()) {
                appendLine("A:")
                appendLine(q.answer)
            }
            q.sourceUrl?.let {
                appendLine()
                appendLine("来源: $it")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, q.question)
        }
        context.startActivity(Intent.createChooser(intent, "分享问题"))
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
