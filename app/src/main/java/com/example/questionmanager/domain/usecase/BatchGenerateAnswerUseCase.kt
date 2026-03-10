package com.example.questionmanager.domain.usecase

import android.util.Log
import com.example.questionmanager.data.repository.AiRepository
import com.example.questionmanager.data.repository.QuestionRepository
import com.example.questionmanager.domain.model.QuestionStatus
import com.example.questionmanager.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 批量会话状态
 */
data class BatchSessionState(
    val sessionId: String,
    val totalCount: Int,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val isActive: Boolean = true,
    val failedIds: List<Long> = emptyList()
) {
    val successCount: Int get() = completedCount - failedCount
    val progress: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
    val isFinished: Boolean get() = completedCount >= totalCount
}

/**
 * 批量生成答案用例
 *
 * 核心设计：
 * 1. 每个批次创建一个独立的 SupervisorJob 作用域，单个任务失败不影响其他任务
 * 2. 使用独立的并发信号量控制每批次的并行度，避免与其他 AI 操作竞争
 * 3. 每个问题通过独立的 API 调用（独立对话）并行生成答案
 * 4. 内置指数退避重试机制，提高大批量生成的成功率
 * 5. 支持取消、进度追踪和失败重试
 */
@Singleton
class BatchGenerateAnswerUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val questionRepository: QuestionRepository
) {
    companion object {
        private const val TAG = "BatchGenerate"
    }

    /** 活跃的批次 Job，支持取消 */
    private val activeSessions = ConcurrentHashMap<String, Job>()

    /** 批次状态 Flow，UI 可以观察进度 */
    private val _sessionStates = MutableStateFlow<Map<String, BatchSessionState>>(emptyMap())
    val sessionStates: StateFlow<Map<String, BatchSessionState>> = _sessionStates.asStateFlow()

    /**
     * 启动一个批量生成会话
     *
     * @param questionIds 要生成答案的问题 ID 列表
     * @param systemPrompt 系统提示词
     * @param scope 协程作用域（通常是 viewModelScope）
     * @param maxConcurrent 最大并发数，每个批次独立控制
     * @param onComplete 全部完成时的回调
     * @return sessionId 可用于取消或查询进度
     */
    fun startBatch(
        questionIds: List<Long>,
        systemPrompt: String,
        scope: CoroutineScope,
        maxConcurrent: Int = Constants.BATCH_MAX_CONCURRENT,
        onComplete: ((BatchSessionState) -> Unit)? = null
    ): String {
        val sessionId = "batch_${System.currentTimeMillis()}_${questionIds.hashCode()}"
        val totalCount = questionIds.size

        // 初始化会话状态
        updateSessionState(sessionId, BatchSessionState(
            sessionId = sessionId,
            totalCount = totalCount
        ))

        val job = scope.launch(Dispatchers.IO + SupervisorJob()) {
            val batchSemaphore = Semaphore(permits = maxConcurrent)
            val completed = AtomicInteger(0)
            val failed = AtomicInteger(0)
            val failedIds = ConcurrentHashMap.newKeySet<Long>()

            // 先将所有问题标记为 GENERATING
            questionIds.forEach { id ->
                try {
                    questionRepository.updateAnswer(id, null, QuestionStatus.GENERATING)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update status for question $id", e)
                }
            }

            // 并行发起独立对话
            val deferreds = questionIds.map { questionId ->
                async(SupervisorJob()) {
                    batchSemaphore.withPermit {
                        generateSingleAnswer(questionId, systemPrompt)
                    }.also { success ->
                        val c = completed.incrementAndGet()
                        if (!success) {
                            failed.incrementAndGet()
                            failedIds.add(questionId)
                        }
                        updateSessionState(sessionId, BatchSessionState(
                            sessionId = sessionId,
                            totalCount = totalCount,
                            completedCount = c,
                            failedCount = failed.get(),
                            isActive = c < totalCount,
                            failedIds = failedIds.toList()
                        ))
                    }
                }
            }

            // 等待全部完成（SupervisorJob 保证单个失败不会取消其他）
            deferreds.awaitAll()

            val finalState = BatchSessionState(
                sessionId = sessionId,
                totalCount = totalCount,
                completedCount = completed.get(),
                failedCount = failed.get(),
                isActive = false,
                failedIds = failedIds.toList()
            )
            updateSessionState(sessionId, finalState)
            onComplete?.invoke(finalState)

            // 清理
            activeSessions.remove(sessionId)
        }

        activeSessions[sessionId] = job
        return sessionId
    }

    /**
     * 为单个问题生成答案，带指数退避重试
     * 每个问题是一个完全独立的 API 对话
     *
     * @return true 表示成功，false 表示最终失败
     */
    private suspend fun generateSingleAnswer(questionId: Long, systemPrompt: String): Boolean {
        val question = questionRepository.getQuestionById(questionId)
            ?: run {
                Log.e(TAG, "Question $questionId not found")
                return false
            }

        return try {
            val result = aiRepository.generateAnswerWithRetry(
                question = question.question,
                systemPrompt = systemPrompt,
                maxRetries = Constants.BATCH_MAX_RETRIES,
                initialDelayMs = Constants.BATCH_RETRY_INITIAL_DELAY_MS
            )
            result.onSuccess { answer ->
                questionRepository.updateAnswer(questionId, answer, QuestionStatus.COMPLETED)
            }.onFailure { e ->
                Log.e(TAG, "Failed to generate answer for question $questionId: ${e.message}")
                questionRepository.updateAnswer(questionId, null, QuestionStatus.ERROR)
            }
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error for question $questionId", e)
            try {
                questionRepository.updateAnswer(questionId, null, QuestionStatus.ERROR)
            } catch (_: Exception) { }
            false
        }
    }

    /**
     * 重试批次中失败的问题
     */
    fun retryFailed(
        sessionId: String,
        systemPrompt: String,
        scope: CoroutineScope,
        maxConcurrent: Int = Constants.BATCH_MAX_CONCURRENT,
        onComplete: ((BatchSessionState) -> Unit)? = null
    ): String? {
        val currentState = _sessionStates.value[sessionId] ?: return null
        if (currentState.failedIds.isEmpty()) return null

        return startBatch(
            questionIds = currentState.failedIds,
            systemPrompt = systemPrompt,
            scope = scope,
            maxConcurrent = maxConcurrent,
            onComplete = onComplete
        )
    }

    /**
     * 取消指定批次
     */
    fun cancelBatch(sessionId: String) {
        activeSessions[sessionId]?.cancel()
        activeSessions.remove(sessionId)
        val currentState = _sessionStates.value[sessionId]
        if (currentState != null) {
            updateSessionState(sessionId, currentState.copy(isActive = false))
        }
    }

    /**
     * 清理已完成的会话状态
     */
    fun clearSession(sessionId: String) {
        activeSessions.remove(sessionId)
        _sessionStates.value = _sessionStates.value - sessionId
    }

    private fun updateSessionState(sessionId: String, state: BatchSessionState) {
        _sessionStates.value = _sessionStates.value + (sessionId to state)
    }
}

