package com.example.questionmanager.domain.usecase

import com.example.questionmanager.data.repository.QuestionRepository
import com.example.questionmanager.domain.model.Question
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 搜索问题用例
 * 封装搜索逻辑
 */
class SearchQuestionsUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    /**
     * 搜索问题
     */
    operator fun invoke(query: String): Flow<List<Question>> {
        return questionRepository.searchQuestions(query)
    }
}

