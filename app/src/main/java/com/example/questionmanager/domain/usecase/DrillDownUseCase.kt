package com.example.questionmanager.domain.usecase

import com.example.questionmanager.data.repository.AiRepository
import com.example.questionmanager.data.repository.QuestionRepository
import com.example.questionmanager.domain.model.Question
import com.example.questionmanager.domain.model.QuestionStatus
import com.example.questionmanager.domain.model.RelationType
import javax.inject.Inject

/**
 * 深挖问题用例
 * 封装深挖功能的完整业务流程：生成引申问题 → 创建条目 → 建立链接 → 生成答案
 */
class DrillDownUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val questionRepository: QuestionRepository
) {
    /**
     * 为指定问题生成引申问题列表
     */
    suspend fun generateSuggestions(questionId: Long): Result<List<String>> {
        val question = questionRepository.getQuestionById(questionId)
            ?: return Result.failure(Exception("问题不存在"))

        return aiRepository.generateDrillDownQuestions(
            question = question.question,
            answer = question.answer ?: ""
        )
    }

    /**
     * 确认选中的引申问题，创建条目并建立链接
     * @return 新创建的条目 ID 列表
     */
    suspend fun confirmSelected(
        parentId: Long,
        selectedQuestions: List<String>,
        promptId: Long? = null
    ): List<Long> {
        val newIds = mutableListOf<Long>()
        for (questionText in selectedQuestions) {
            val newQuestion = Question(
                question = questionText,
                status = QuestionStatus.PENDING,
                promptId = promptId
            )
            val newId = questionRepository.insertQuestion(newQuestion)
            questionRepository.createLink(parentId, newId, RelationType.DRILL_DOWN)
            newIds.add(newId)
        }
        return newIds
    }
}

