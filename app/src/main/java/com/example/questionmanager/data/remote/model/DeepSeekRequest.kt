package com.example.questionmanager.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DeepSeek Chat Completions 请求体
 * 文档: https://api-docs.deepseek.com/api/create-chat-completion
 *
 * 仅包含必要字段，避免发送多余参数
 */
@Serializable
data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    val messages: List<Message>,
    val temperature: Double = 1.0,
    @SerialName("max_tokens")
    val maxTokens: Int = 2048,
    val stream: Boolean = false
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

