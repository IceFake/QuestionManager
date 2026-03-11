package com.example.questionmanager.domain.usecase

import com.example.questionmanager.data.repository.AiRepository
import com.example.questionmanager.data.repository.QuestionRepository
import com.example.questionmanager.domain.model.Question
import com.example.questionmanager.domain.model.QuestionStatus
import com.example.questionmanager.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import javax.inject.Inject

/**
 * 生成答案用例
 * 封装 AI 答案生成的业务逻辑
 */
class GenerateAnswerUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val questionRepository: QuestionRepository
) {
    /**
     * 为指定问题生成答案 (非流式)
     */
    suspend operator fun invoke(questionId: Long, systemPrompt: String? = null): Result<String> {
        val question = questionRepository.getQuestionById(questionId)
            ?: return Result.failure(Exception("问题不存在"))

        val prompt = systemPrompt
            ?: questionRepository.getDefaultPrompt()?.systemPrompt
            ?: Constants.DEFAULT_SYSTEM_PROMPT

        questionRepository.updateAnswer(questionId, null, QuestionStatus.GENERATING)

        val ancestors = questionRepository.getAllAncestors(questionId)
        val ancestorContext = buildAncestorContext(ancestors)

        val result = aiRepository.generateAnswer(question.question, prompt, ancestorContext)

        result.onSuccess { answer ->
            questionRepository.updateAnswer(questionId, answer, QuestionStatus.COMPLETED)
        }.onFailure {
            questionRepository.updateAnswer(questionId, null, QuestionStatus.ERROR)
        }

        return result
    }

    /**
     * 流式生成答案 — 逐步发射部分文本，最后写入 DB
     * @return Flow<String> 每次发射累计的答案文本
     */
    fun stream(questionId: Long, systemPrompt: String? = null): Flow<String> = flow {
        val question = questionRepository.getQuestionById(questionId)
            ?: throw Exception("问题不存在")

        val prompt = systemPrompt
            ?: questionRepository.getDefaultPrompt()?.systemPrompt
            ?: Constants.DEFAULT_SYSTEM_PROMPT

        questionRepository.updateAnswer(questionId, null, QuestionStatus.GENERATING)

        val ancestors = questionRepository.getAllAncestors(questionId)
        val ancestorContext = buildAncestorContext(ancestors)

        var lastContent = ""
        aiRepository.generateAnswerStream(question.question, prompt, ancestorContext)
            .onCompletion { error ->
                if (error == null && lastContent.isNotBlank()) {
                    questionRepository.updateAnswer(questionId, lastContent, QuestionStatus.COMPLETED)
                } else if (error != null) {
                    questionRepository.updateAnswer(questionId, null, QuestionStatus.ERROR)
                }
            }
            .catch { e ->
                questionRepository.updateAnswer(questionId, null, QuestionStatus.ERROR)
                throw e
            }
            .collect { partial ->
                lastContent = partial
                emit(partial)
            }
    }

    private fun buildAncestorContext(ancestors: List<Question>): String {
        if (ancestors.isEmpty()) return ""
        
        return buildString {
            ancestors.forEachIndexed { index, ancestor ->
                append("${index + 1}. 问题：${ancestor.question}")
                if (!ancestor.answer.isNullOrEmpty()) {
                    append("\n   答案：${ancestor.answer}")
                }
                append("\n")
            }
        }.trim()
    }
}

