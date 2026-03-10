package com.example.questionmanager.domain.model

import com.example.questionmanager.data.local.db.entity.PromptEntity

/**
 * 提示词领域模型
 */
data class Prompt(
    val id: Long = 0,
    val name: String,
    val systemPrompt: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity → Domain 转换
 */
fun PromptEntity.toDomain(): Prompt = Prompt(
    id = id,
    name = name,
    systemPrompt = systemPrompt,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Domain → Entity 转换
 */
fun Prompt.toEntity(): PromptEntity = PromptEntity(
    id = id,
    name = name,
    systemPrompt = systemPrompt,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt
)
