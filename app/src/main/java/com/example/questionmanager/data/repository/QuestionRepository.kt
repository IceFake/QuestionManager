package com.example.questionmanager.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.questionmanager.data.local.db.dao.PromptDao
import com.example.questionmanager.data.local.db.dao.QuestionDao
import com.example.questionmanager.data.local.db.dao.QuestionLinkDao
import com.example.questionmanager.data.local.db.entity.QuestionLinkEntity
import com.example.questionmanager.domain.model.Prompt
import com.example.questionmanager.domain.model.Question
import com.example.questionmanager.domain.model.QuestionStatus
import com.example.questionmanager.domain.model.RelationType
import com.example.questionmanager.domain.model.toDomain
import com.example.questionmanager.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val questionDao: QuestionDao,
    private val questionLinkDao: QuestionLinkDao,
    private val promptDao: PromptDao
) {
    // ── 问题条目 CRUD ──

    fun getAllQuestions(): Flow<List<Question>> {
        return questionDao.getAllQuestions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 分页获取问题列表 (Paging3)
     */
    fun getAllQuestionsPaged(): Flow<PagingData<Question>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { questionDao.getAllQuestionsPaged() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    suspend fun getQuestionById(id: Long): Question? {
        return questionDao.getQuestionById(id)?.toDomain()
    }

    suspend fun insertQuestion(question: Question): Long {
        return questionDao.insertQuestion(question.toEntity())
    }

    suspend fun insertQuestions(questions: List<Question>): List<Long> {
        return questionDao.insertQuestions(questions.map { it.toEntity() })
    }

    suspend fun updateQuestion(question: Question) {
        questionDao.updateQuestion(question.toEntity())
    }

    suspend fun deleteQuestion(id: Long) {
        val entity = questionDao.getQuestionById(id) ?: return
        questionDao.deleteQuestion(entity)
    }

    suspend fun updateAnswer(id: Long, answer: String?, status: QuestionStatus) {
        questionDao.updateAnswer(id, answer, status.value)
    }

    // ── 链接关系 ──

    fun getChildQuestions(parentId: Long): Flow<List<Question>> {
        return questionLinkDao.getLinkedChildQuestions(parentId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getParentQuestions(childId: Long): Flow<List<Question>> {
        return questionLinkDao.getLinkedParentQuestions(childId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun createLink(parentId: Long, childId: Long, type: RelationType = RelationType.DRILL_DOWN) {
        questionLinkDao.insertLink(
            QuestionLinkEntity(
                parentId = parentId,
                childId = childId,
                relationType = type.value
            )
        )
    }

    suspend fun removeLink(parentId: Long, childId: Long) {
        questionLinkDao.deleteLinkByParentAndChild(parentId, childId)
    }

    // ── 搜索 ──

    /**
     * 搜索问题 — 先尝试 FTS4 全文搜索，失败时降级为 LIKE 搜索
     */
    fun searchQuestions(query: String): Flow<List<Question>> {
        return try {
            // FTS match query: 使用 * 通配符支持前缀匹配
            val ftsQuery = query.trim().replace(Regex("\\s+"), " ") + "*"
            questionDao.searchQuestionsFts(ftsQuery).map { entities ->
                entities.map { it.toDomain() }
            }
        } catch (_: Exception) {
            // FTS 不可用时降级为 LIKE
            questionDao.searchQuestions(query).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    // ── 提示词 ──

    fun getAllPrompts(): Flow<List<Prompt>> {
        return promptDao.getAllPrompts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getDefaultPrompt(): Prompt? {
        return promptDao.getDefaultPrompt()?.toDomain()
    }

    suspend fun insertPrompt(prompt: Prompt): Long {
        return promptDao.insertPrompt(prompt.toEntity())
    }

    suspend fun updatePrompt(prompt: Prompt) {
        promptDao.updatePrompt(prompt.toEntity())
    }

    suspend fun deletePrompt(id: Long) {
        val entity = promptDao.getPromptById(id) ?: return
        promptDao.deletePrompt(entity)
    }

    suspend fun setDefaultPrompt(id: Long) {
        promptDao.clearDefaultPrompt()
        val entity = promptDao.getPromptById(id) ?: return
        promptDao.updatePrompt(entity.copy(isDefault = true, updatedAt = System.currentTimeMillis()))
    }
}

