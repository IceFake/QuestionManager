package com.example.questionmanager.domain.usecase

import com.example.questionmanager.data.repository.AiRepository
import javax.inject.Inject

/**
 * 解析网址用例
 * 将 URL 直接发送给 AI，由 AI 总结出问题列表
 */
class ParseUrlUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    /**
     * 解析 URL 并返回问题列表
     * 支持完整 URL (http://..., https://...) 和裸域名
     */
    suspend operator fun invoke(url: String): Result<List<String>> {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("URL 不能为空"))
        }
        // 自动补全协议头
        val normalizedUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> "https://$trimmed"
        }
        return aiRepository.parseQuestionsFromUrl(normalizedUrl)
    }
}

