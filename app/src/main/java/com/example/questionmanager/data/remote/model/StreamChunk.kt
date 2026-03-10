package com.example.questionmanager.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SSE 流式响应数据块
 * DeepSeek API 在 stream=true 时返回的每个 data: 行
 * 文档: https://api-docs.deepseek.com/api/create-chat-completion (stream)
 */
@Serializable
data class StreamChunk(
    val id: String = "",
    @SerialName("object")
    val objectType: String = "",
    val created: Long = 0L,
    val model: String = "",
    val choices: List<StreamChoice> = emptyList(),
    val usage: Usage? = null
)

@Serializable
data class StreamChoice(
    val index: Int = 0,
    val delta: StreamDelta = StreamDelta(),
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class StreamDelta(
    val role: String? = null,
    val content: String? = null
)

