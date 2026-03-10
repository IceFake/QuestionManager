package com.example.questionmanager.domain.model

import com.example.questionmanager.data.local.db.entity.QuestionEntity

/**
 * 问题状态枚举
 */
enum class QuestionStatus(val value: String) {
    PENDING("PENDING"),
    GENERATING("GENERATING"),
    COMPLETED("COMPLETED"),
    ERROR("ERROR");

    companion object {
        fun fromValue(value: String): QuestionStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}

/**
 * 问题领域模型
 */
data class Question(
    val id: Long = 0,
    val question: String,
    val answer: String? = null,
    val status: QuestionStatus = QuestionStatus.PENDING,
    val promptId: Long? = null,
    val sourceUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity → Domain 转换
 */
fun QuestionEntity.toDomain(): Question = Question(
    id = id,
    question = question,
    answer = answer,
    status = QuestionStatus.fromValue(status),
    promptId = promptId,
    sourceUrl = sourceUrl,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Domain → Entity 转换
 */
fun Question.toEntity(): QuestionEntity = QuestionEntity(
    id = id,
    question = question,
    answer = answer,
    status = status.value,
    promptId = promptId,
    sourceUrl = sourceUrl,
    createdAt = createdAt,
    updatedAt = updatedAt
)
