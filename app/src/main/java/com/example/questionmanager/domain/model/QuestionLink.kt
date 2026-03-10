package com.example.questionmanager.domain.model

import com.example.questionmanager.data.local.db.entity.QuestionLinkEntity

/**
 * 链接关系类型枚举
 */
enum class RelationType(val value: String) {
    DRILL_DOWN("DRILL_DOWN"),
    RELATED("RELATED");

    companion object {
        fun fromValue(value: String): RelationType {
            return entries.find { it.value == value } ?: DRILL_DOWN
        }
    }
}

/**
 * 条目链接领域模型
 */
data class QuestionLink(
    val id: Long = 0,
    val parentId: Long,
    val childId: Long,
    val relationType: RelationType = RelationType.DRILL_DOWN,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entity → Domain 转换
 */
fun QuestionLinkEntity.toDomain(): QuestionLink = QuestionLink(
    id = id,
    parentId = parentId,
    childId = childId,
    relationType = RelationType.fromValue(relationType),
    createdAt = createdAt
)

/**
 * Domain → Entity 转换
 */
fun QuestionLink.toEntity(): QuestionLinkEntity = QuestionLinkEntity(
    id = id,
    parentId = parentId,
    childId = childId,
    relationType = relationType.value,
    createdAt = createdAt
)
