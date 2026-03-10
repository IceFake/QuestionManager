package com.example.questionmanager.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.questionmanager.data.repository.QuestionRepository
import com.example.questionmanager.domain.model.Question
import com.example.questionmanager.domain.model.QuestionStatus
import com.example.questionmanager.domain.usecase.GenerateAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val generateAnswerUseCase: GenerateAnswerUseCase
) : ViewModel() {

    data class HomeUiState(
        val questions: List<Question> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
        val isSelectionMode: Boolean = false,
        val selectedIds: Set<Long> = emptySet(),
        val isBatchProcessing: Boolean = false
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Paging3 分页数据流 — 大数据量列表使用
     */
    val pagedQuestions: Flow<PagingData<Question>> =
        questionRepository.getAllQuestionsPaged().cachedIn(viewModelScope)

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            questionRepository.getAllQuestions()
                .onStart {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
                .collect { questions ->
                    _uiState.value = _uiState.value.copy(
                        questions = questions,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun deleteQuestion(id: Long) {
        viewModelScope.launch {
            try {
                questionRepository.deleteQuestion(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "删除失败: ${e.message}")
            }
        }
    }

    // ── 选择模式 ──

    fun toggleSelectionMode() {
        val current = _uiState.value.isSelectionMode
        _uiState.value = _uiState.value.copy(
            isSelectionMode = !current,
            selectedIds = emptySet()
        )
    }

    fun exitSelectionMode() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    fun toggleSelection(id: Long) {
        val current = _uiState.value.selectedIds.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _uiState.value = _uiState.value.copy(selectedIds = current)
    }

    fun selectAll() {
        val allIds = _uiState.value.questions.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedIds = allIds)
    }

    fun deselectAll() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    // ── 批量操作 ──

    fun batchDelete() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchProcessing = true)
            try {
                ids.forEach { id ->
                    questionRepository.deleteQuestion(id)
                }
                _uiState.value = _uiState.value.copy(
                    isBatchProcessing = false,
                    isSelectionMode = false,
                    selectedIds = emptySet()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBatchProcessing = false,
                    error = "批量删除失败: ${e.message}"
                )
            }
        }
    }

    fun batchGenerate() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchProcessing = true)
            try {
                ids.forEach { id ->
                    val question = questionRepository.getQuestionById(id)
                    if (question != null && question.status != QuestionStatus.GENERATING) {
                        launch { generateAnswerUseCase(id) }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isBatchProcessing = false,
                    isSelectionMode = false,
                    selectedIds = emptySet()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBatchProcessing = false,
                    error = "批量生成失败: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

