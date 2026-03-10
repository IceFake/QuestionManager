package com.example.questionmanager.domain.usecase

import com.example.questionmanager.data.repository.AiRepository
import javax.inject.Inject

/**
 * 解析网址用例
 * 从 URL 抓取网页内容并提取问题列表
 */
class ParseUrlUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    /**
     * 解析 URL 并返回问题列表
     */
    suspend operator fun invoke(url: String): Result<List<String>> {
        if (url.isBlank()) {
            return Result.failure(IllegalArgumentException("URL 不能为空"))
        }
        return aiRepository.parseQuestionsFromUrl(url)
    }
}

