package com.example.questionmanager.domain.usecase

import com.example.questionmanager.data.repository.AiRepository
import javax.inject.Inject

/**
 * 解析文本内容用例
 * 将用户输入的文本内容发送给 AI，由 AI 整理成结构化的问题条目
 */
class ParseContentUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    /**
     * 解析文本内容并返回问题列表
     */
    suspend operator fun invoke(content: String): Result<List<String>> {
        val trimmed = content.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("内容不能为空"))
        }
        return aiRepository.parseContentToQuestions(trimmed)
    }
}

