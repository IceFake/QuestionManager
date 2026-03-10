package com.example.questionmanager.data.repository

import com.example.questionmanager.data.remote.api.DeepSeekApiService
import com.example.questionmanager.data.remote.api.WebParserService
import com.example.questionmanager.data.remote.model.DeepSeekRequest
import com.example.questionmanager.data.remote.model.Message
import com.example.questionmanager.data.remote.model.StreamChunk
import com.example.questionmanager.util.AiResponseParser
import com.example.questionmanager.util.Constants
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
    /**
     * 并发限流信号量
     * 限制同时进行的 AI API 请求数量，避免触发 Rate Limit (429)
     */
    private val apiSemaphore = Semaphore(permits = Constants.AI_MAX_CONCURRENT_REQUESTS)

    /**
     * 为问题生成答案 (受限流保护)
     */
    suspend fun generateAnswer(question: String, systemPrompt: String): Result<String> {
        return apiSemaphore.withPermit {
            try {
                val apiKey = settingsRepository.apiKeyFlow.first()
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
                if (e.code() == 429) {
                    // Rate Limit: 等待后重试一次
                    delay(5000)
                    return@withPermit generateAnswer(question, systemPrompt)
                }
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
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
     * 基于问题和答案生成引申问题列表 (受限流保护)
     * 响应解析使用 AiResponseParser 进行多策略容错
     */
    suspend fun generateDrillDownQuestions(question: String, answer: String): Result<List<String>> {
        return apiSemaphore.withPermit {
            try {
                val apiKey = settingsRepository.apiKeyFlow.first()
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
                if (e.code() == 429) {
                    delay(5000)
                    return@withPermit generateDrillDownQuestions(question, answer)
                }
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 从网页内容中解析出问题列表
     * 先用 Jsoup 抓取内容，再调用 AI 提取问题
     */
    suspend fun parseQuestionsFromUrl(url: String): Result<List<String>> {
        return try {
            val webContent = webParserService.fetchAndParse(url).getOrThrow()
            val apiKey = settingsRepository.apiKeyFlow.first()
            val model = settingsRepository.modelFlow.first()

            val systemPrompt = """你是一个内容分析助手。请从给定的网页文本中提取出所有问题。
严格要求：
1. 仅返回一个合法的 JSON 数组，不要包含任何其他文字、解释或 Markdown 格式
2. 数组中每个元素是一个纯字符串（问题文本）
3. 示例格式：["问题1", "问题2", "问题3"]"""

            val request = DeepSeekRequest(
                model = model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = "请从以下网页内容中提取问题：\n\n$webContent")
                )
            )
            val response = deepSeekApiService.chatCompletion(
                authHeader = "Bearer $apiKey",
                request = request
            )
            val rawContent = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("AI 返回内容为空"))

            val parsedQuestions = AiResponseParser.parseDrillDownQuestions(rawContent)
            Result.success(parsedQuestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val streamJson = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * 流式生成答案 — SSE (Server-Sent Events)
     * 逐步发射部分答案文本，调用方可实时更新 UI
     */
    fun generateAnswerStream(question: String, systemPrompt: String): Flow<String> = flow {
        apiSemaphore.withPermit {
            val apiKey = settingsRepository.apiKeyFlow.first()
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

