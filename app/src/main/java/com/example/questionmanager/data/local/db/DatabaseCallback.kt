package com.example.questionmanager.data.local.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.questionmanager.data.local.db.dao.PromptDao
import com.example.questionmanager.data.local.db.entity.PromptEntity
import com.example.questionmanager.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 数据库创建回调
 * 在数据库首次创建时插入默认 seed 数据
 */
class DatabaseCallback(
    private val promptDaoProvider: () -> PromptDao
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            seedDefaultPrompts(promptDaoProvider())
        }
    }

    private suspend fun seedDefaultPrompts(promptDao: PromptDao) {
        val defaultPrompts = listOf(
            PromptEntity(
                name = "默认",
                systemPrompt = Constants.DEFAULT_SYSTEM_PROMPT,
                isDefault = true
            ),
            PromptEntity(
                name = "学术风格",
                systemPrompt = "你是一位严谨的学术研究助手。请用学术论文的风格回答问题，注重逻辑推理、引用依据和专业术语的准确使用。使用 Markdown 格式来组织你的回答，包含清晰的标题层级和有序列表。",
                isDefault = false
            ),
            PromptEntity(
                name = "简洁回答",
                systemPrompt = "你是一个高效的助手。请用简洁明了的语言回答问题，避免冗余内容。每个要点用一到两句话概括，使用 Markdown 格式的列表来组织回答。",
                isDefault = false
            ),
            PromptEntity(
                name = "深入讲解",
                systemPrompt = "你是一位经验丰富的教师。请深入浅出地讲解问题，适当使用类比和示例来帮助理解。如果涉及代码，请提供可运行的示例。使用 Markdown 格式来组织你的回答。",
                isDefault = false
            )
        )
        defaultPrompts.forEach { prompt ->
            promptDao.insertPrompt(prompt)
        }
    }
}

