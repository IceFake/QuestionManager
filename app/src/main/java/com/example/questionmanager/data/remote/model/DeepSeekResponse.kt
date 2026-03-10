package com.example.questionmanager.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DeepSeek Chat Completions 响应
 * 文档: https://api-docs.deepseek.com/api/create-chat-completion
 *
 * 仅保留实际使用的字段。Json 配置了 ignoreUnknownKeys = true，
 * API 返回的其他字段（object, created, model, system_fingerprint 等）会被安全忽略。
 */
@Serializable
data class DeepSeekResponse(
    val id: String = "",
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: Message,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)
