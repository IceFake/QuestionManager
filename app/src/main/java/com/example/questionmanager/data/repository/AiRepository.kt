package com.example.questionmanager.data.repository

import android.util.Log
import com.example.questionmanager.data.remote.api.DeepSeekApiService
import com.example.questionmanager.data.remote.api.WebParserService
import com.example.questionmanager.data.remote.model.DeepSeekRequest
import com.example.questionmanager.data.remote.model.Message
import com.example.questionmanager.data.remote.model.StreamChunk
import com.example.questionmanager.di.DynamicBaseUrl
import com.example.questionmanager.util.AiResponseParser
import com.example.questionmanager.util.Constants
import com.example.questionmanager.util.RetryUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val deepSeekApiService: DeepSeekApiService,
    private val webParserService: WebParserService,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "AiRepository"
    }
    /**
     * 并发限流信号量
     * 限制同时进行的 AI API 请求数量，避免触发 Rate Limit (429)
     */
    private val apiSemaphore = Semaphore(permits = Constants.AI_MAX_CONCURRENT_REQUESTS)

    /**
     * 同步 Base URL 到 OkHttp 动态拦截器
     * 每次 API 调用前执行，确保使用用户设置的最新 URL
     */
    private suspend fun syncBaseUrl() {
        val baseUrl = settingsRepository.baseUrlFlow.first()
        if (baseUrl.isNotBlank()) {
            DynamicBaseUrl.baseUrl = baseUrl
        }
    }

    /**
     * 为问题生成答案 (受限流保护)
     */
    suspend fun generateAnswer(question: String, systemPrompt: String): Result<String> {
        return apiSemaphore.withPermit {
            try {
                syncBaseUrl()
                val apiKey = settingsRepository.apiKeyFlow.first()

                if (apiKey.isBlank()) {
                    return@withPermit Result.failure(Exception("请先在设置中配置 API Key"))
                }

                val model = settingsRepository.modelFlow.first()
                val temperature = settingsRepository.temperatureFlow.first()
                val maxTokens = settingsRepository.maxTokensFlow.first()

                val request = DeepSeekRequest(
                    model = model,
                    messages = listOf(
                        Message(role = "system", content = systemPrompt),
                        Message(role = "user", content = "请回答以下问题：\n\n$question")
                    ),
                    temperature = temperature,
                    maxTokens = maxTokens
                )
                val response = deepSeekApiService.chatCompletion(
                    authHeader = "Bearer $apiKey",
                    request = request
                )
                val answer = response.choices.firstOrNull()?.message?.content
                    ?: return@withPermit Result.failure(Exception("AI 返回内容为空"))
                Result.success(answer)
            } catch (e: HttpException) {
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                Log.e(TAG, "HTTP ${e.code()} error: $errorBody", e)
                if (e.code() == 429) {
                    delay(5000)
                    return@withPermit generateAnswer(question, systemPrompt)
                }
                val detail = if (errorBody != null) "$errorBody" else ""
                Result.failure(Exception("${RetryUtil.friendlyErrorMessage(e)} $detail".trim()))
            } catch (e: Exception) {
                Log.e(TAG, "generateAnswer error: ${e.javaClass.simpleName}: ${e.message}", e)
                Result.failure(Exception(RetryUtil.friendlyErrorMessage(e)))
            }
        }
    }

    /**
     * 批量为问题生成答案 (内部使用 Semaphore 自动限流)
     */
    suspend fun generateAnswersBatch(
        questions: List<Pair<Long, String>>,
        systemPrompt: String,
        onEachResult: suspend (id: Long, Result<String>) -> Unit
    ) = coroutineScope {
        questions.map { (id, question) ->
            async {
                val result = generateAnswer(question, systemPrompt)
                onEachResult(id, result)
            }
        }.awaitAll()
    }

    /**
     * 带指数退避重试的答案生成
     * 每次调用是一个完全独立的 API 对话，不共享上下文。
     * 专为批量并行场景设计：
     *   - 不经过全局 apiSemaphore（由调用方通过批次级信号量控制并发）
     *   - 内置指数退避重试，针对 429/5xx 等可重试错误
     */
    suspend fun generateAnswerWithRetry(
        question: String,
        systemPrompt: String,
        maxRetries: Int = Constants.BATCH_MAX_RETRIES,
        initialDelayMs: Long = Constants.BATCH_RETRY_INITIAL_DELAY_MS
    ): Result<String> {
        return RetryUtil.withRetry(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            maxDelayMs = Constants.BATCH_RETRY_MAX_DELAY_MS,
            factor = 2.0,
            shouldRetry = { e ->
                e is HttpException && RetryUtil.isRetryableHttpError(e.code())
            }
        ) {
            syncBaseUrl()
            val apiKey = settingsRepository.apiKeyFlow.first()
            if (apiKey.isBlank()) {
                throw Exception("请先在设置中配置 API Key")
            }

            val model = settingsRepository.modelFlow.first()
            val temperature = settingsRepository.temperatureFlow.first()
            val maxTokens = settingsRepository.maxTokensFlow.first()

            val request = DeepSeekRequest(
                model = model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = "请回答以下问题：\n\n$question")
                ),
                temperature = temperature,
                maxTokens = maxTokens
            )
            val response = deepSeekApiService.chatCompletion(
                authHeader = "Bearer $apiKey",
                request = request
            )
            val answer = response.choices.firstOrNull()?.message?.content
                ?: throw Exception("AI 返回内容为空")
            Result.success(answer)
        }
    }

    /**
     * 基于问题和答案生成引申问题列表 (受限流保护)
     * 响应解析使用 AiResponseParser 进行多策略容错
     */
    suspend fun generateDrillDownQuestions(question: String, answer: String): Result<List<String>> {
        return apiSemaphore.withPermit {
            try {
                syncBaseUrl()
                val apiKey = settingsRepository.apiKeyFlow.first()

                if (apiKey.isBlank()) {
                    return@withPermit Result.failure(Exception("请先在设置中配置 API Key"))
                }

                val model = settingsRepository.modelFlow.first()

                val systemPrompt = """你是一个问题分析助手。请基于给定的问题，生成 5 个有深度的引申问题。
严格要求：
1. 仅返回一个合法的 JSON 数组，不要包含任何其他文字、解释或 Markdown 格式
2. 数组中每个元素是一个纯字符串
3. 示例格式：["问题1", "问题2", "问题3", "问题4", "问题5"]"""

                val request = DeepSeekRequest(
                    model = model,
                    messages = listOf(
                        Message(role = "system", content = systemPrompt),
                        Message(role = "user", content = "原始问题：$question\n\n已有答案：$answer\n\n请生成引申问题。")
                    )
                )
                val response = deepSeekApiService.chatCompletion(
                    authHeader = "Bearer $apiKey",
                    request = request
                )
                val rawContent = response.choices.firstOrNull()?.message?.content
                    ?: return@withPermit Result.failure(Exception("AI 返回内容为空"))

                val parsedQuestions = AiResponseParser.parseDrillDownQuestions(rawContent)
                Result.success(parsedQuestions)
            } catch (e: HttpException) {
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                Log.e(TAG, "generateDrillDownQuestions HTTP ${e.code()}: $errorBody", e)
                if (e.code() == 429) {
                    delay(5000)
                    return@withPermit generateDrillDownQuestions(question, answer)
                }
                val detail = if (errorBody != null) "$errorBody" else ""
                Result.failure(Exception("${RetryUtil.friendlyErrorMessage(e)} $detail".trim()))
            } catch (e: Exception) {
                Log.e(TAG, "generateDrillDownQuestions error: ${e.javaClass.simpleName}: ${e.message}", e)
                Result.failure(Exception(RetryUtil.friendlyErrorMessage(e)))
            }
        }
    }

    /**
     * 从 URL 解析问题列表
     * 步骤:
     *   1. 通过 WebParserService (Jsoup / WebView) 抓取网页文本内容
     *   2. 将抓取到的文本发送给 AI，由 AI 总结提取问题
     *
     * 注意: DeepSeek 是语言模型，无法自行访问网页，所以必须先本地抓取。
     */
    suspend fun parseQuestionsFromUrl(url: String): Result<List<String>> {
        // 第 1 步: 抓取网页内容
        val webContentResult = try {
            webParserService.fetchAndParse(url)
        } catch (e: Exception) {
            return Result.failure(Exception("网页抓取失败: ${e.message}"))
        }

        val webContent = webContentResult.getOrElse { e ->
            return Result.failure(Exception("网页抓取失败: ${e.message}"))
        }

        // 第 2 步: 发送内容给 AI 提取问题
        return apiSemaphore.withPermit {
            try {
                syncBaseUrl()
                val apiKey = settingsRepository.apiKeyFlow.first()
                val model = settingsRepository.modelFlow.first()

                if (apiKey.isBlank()) {
                    return@withPermit Result.failure(Exception("请先在设置中配置 API Key"))
                }

                val systemPrompt = """你是一个内容分析助手。请从给定的网页文本中提取出有价值的问题。
严格要求：
1. 仅返回一个合法的 JSON 数组，不要包含任何其他文字、解释或 Markdown 格式
2. 数组中每个元素是一个纯字符串（问题文本）
3. 每个问题应当完整、独立、有意义
4. 至少返回 1 个问题，最多不超过 10 个
5. 示例格式：["问题1", "问题2", "问题3"]"""

                // 截断过长的网页内容，防止超出 token 限制
                val truncatedContent = if (webContent.length > 8000) {
                    webContent.take(8000) + "\n\n[内容已截断...]"
                } else {
                    webContent
                }

                val request = DeepSeekRequest(
                    model = model,
                    messages = listOf(
                        Message(role = "system", content = systemPrompt),
                        Message(role = "user", content = "请从以下网页内容中提取问题：\n\n$truncatedContent")
                    )
                )
                val response = deepSeekApiService.chatCompletion(
                    authHeader = "Bearer $apiKey",
                    request = request
                )
                val rawContent = response.choices.firstOrNull()?.message?.content
                    ?: return@withPermit Result.failure(Exception("AI 返回内容为空"))

                val parsedQuestions = AiResponseParser.parseDrillDownQuestions(rawContent)
                if (parsedQuestions.isEmpty()) {
                    return@withPermit Result.failure(Exception("未能从该网页解析出问题"))
                }
                Result.success(parsedQuestions)
            } catch (e: HttpException) {
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                Log.e(TAG, "parseQuestionsFromUrl HTTP ${e.code()}: $errorBody", e)
                if (e.code() == 429) {
                    delay(5000)
                    return@withPermit parseQuestionsFromUrl(url)
                }
                val detail = if (errorBody != null) "$errorBody" else ""
                Result.failure(Exception("${RetryUtil.friendlyErrorMessage(e)} $detail".trim()))
            } catch (e: Exception) {
                Log.e(TAG, "parseQuestionsFromUrl error: ${e.javaClass.simpleName}: ${e.message}", e)
                Result.failure(Exception(RetryUtil.friendlyErrorMessage(e)))
            }
        }
    }

    /**
     * 从用户输入的文本内容中解析出问题列表
     * 将文本发送给 DeepSeek，由 AI 提取/整理成结构化的问题条目
     */
    suspend fun parseContentToQuestions(content: String): Result<List<String>> {
        return apiSemaphore.withPermit {
            try {
                syncBaseUrl()
                val apiKey = settingsRepository.apiKeyFlow.first()
                val model = settingsRepository.modelFlow.first()

                if (apiKey.isBlank()) {
                    return@withPermit Result.failure(Exception("请先在设置中配置 API Key"))
                }

                val systemPrompt = """你是一个内容分析助手。用户会给你一段文本内容，请将其整理成一条或多条清晰、完整、独立的问题。
严格要求：
1. 仅返回一个合法的 JSON 数组，不要包含任何其他文字、解释或 Markdown 格式
2. 数组中每个元素是一个纯字符串（问题文本）
3. 如果用户输入本身就是一个完整问题，直接返回包含该问题的数组
4. 如果用户输入包含多个问题或可以拆分，返回多条
5. 示例格式：["问题1", "问题2", "问题3"]"""

                val request = DeepSeekRequest(
                    model = model,
                    messages = listOf(
                        Message(role = "system", content = systemPrompt),
                        Message(role = "user", content = "请将以下内容整理为问题：\n\n$content")
                    )
                )
                val response = deepSeekApiService.chatCompletion(
                    authHeader = "Bearer $apiKey",
                    request = request
                )
                val rawContent = response.choices.firstOrNull()?.message?.content
                    ?: return@withPermit Result.failure(Exception("AI 返回内容为空"))

                val parsedQuestions = AiResponseParser.parseDrillDownQuestions(rawContent)
                if (parsedQuestions.isEmpty()) {
                    return@withPermit Result.failure(Exception("未能从内容中提取出问题"))
                }
                Result.success(parsedQuestions)
            } catch (e: HttpException) {
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                Log.e(TAG, "parseContentToQuestions HTTP ${e.code()}: $errorBody", e)
                if (e.code() == 429) {
                    delay(5000)
                    return@withPermit parseContentToQuestions(content)
                }
                val detail = if (errorBody != null) "$errorBody" else ""
                Result.failure(Exception("${RetryUtil.friendlyErrorMessage(e)} $detail".trim()))
            } catch (e: Exception) {
                Log.e(TAG, "parseContentToQuestions error: ${e.javaClass.simpleName}: ${e.message}", e)
                Result.failure(Exception(RetryUtil.friendlyErrorMessage(e)))
            }
        }
    }

    private val streamJson = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * 流式生成答案 — SSE (Server-Sent Events)
     * 逐步发射部分答案文本，调用方可实时更新 UI
     */
    fun generateAnswerStream(question: String, systemPrompt: String): Flow<String> = flow {
        apiSemaphore.withPermit {
            syncBaseUrl()
            val apiKey = settingsRepository.apiKeyFlow.first()

            if (apiKey.isBlank()) {
                throw Exception("请先在设置中配置 API Key")
            }

            val model = settingsRepository.modelFlow.first()
            val temperature = settingsRepository.temperatureFlow.first()
            val maxTokens = settingsRepository.maxTokensFlow.first()

            val request = DeepSeekRequest(
                model = model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = "请回答以下问题：\n\n$question")
                ),
                temperature = temperature,
                maxTokens = maxTokens,
                stream = true
            )
            val responseBody = deepSeekApiService.chatCompletionStream(
                authHeader = "Bearer $apiKey",
                request = request
            )
            val reader: BufferedReader = responseBody.byteStream().bufferedReader()
            val sb = StringBuilder()
            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val data = line ?: continue
                    if (!data.startsWith("data: ")) continue
                    val payload = data.removePrefix("data: ").trim()
                    if (payload == "[DONE]") break
                    try {
                        val chunk = streamJson.decodeFromString<StreamChunk>(payload)
                        val content = chunk.choices.firstOrNull()?.delta?.content
                        if (!content.isNullOrEmpty()) {
                            sb.append(content)
                            emit(sb.toString())
                        }
                    } catch (_: Exception) {
                        // skip malformed chunks
                    }
                }
            } finally {
                reader.close()
                responseBody.close()
            }
        }
    }.flowOn(Dispatchers.IO)
}

