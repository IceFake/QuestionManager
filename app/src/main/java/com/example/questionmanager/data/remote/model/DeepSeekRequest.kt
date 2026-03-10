package com.example.questionmanager.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    val messages: List<Message>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens")
    val maxTokens: Int = 2048,
    val stream: Boolean = false
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

