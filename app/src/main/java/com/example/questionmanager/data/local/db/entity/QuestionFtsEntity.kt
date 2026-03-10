package com.example.questionmanager.data.local.db.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * FTS4 全文搜索虚拟表
 * 映射 question_items 表的 question 和 answer 列
 * tokenizer = unicode61 支持中文分词
 */
@Fts4(contentEntity = QuestionEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "question_items_fts")
data class QuestionFtsEntity(
    val question: String,
    val answer: String?
)

