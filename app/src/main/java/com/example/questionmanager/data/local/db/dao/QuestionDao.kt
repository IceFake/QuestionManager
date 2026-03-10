package com.example.questionmanager.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.questionmanager.data.local.db.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM question_items ORDER BY created_at DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    /**
     * Paging3 分页查询 — 按创建时间倒序
     */
    @Query("SELECT * FROM question_items ORDER BY created_at DESC")
    fun getAllQuestionsPaged(): PagingSource<Int, QuestionEntity>

    @Query("SELECT * FROM question_items WHERE id = :id")
    suspend fun getQuestionById(id: Long): QuestionEntity?

    @Query("SELECT * FROM question_items WHERE question LIKE '%' || :query || '%'")
    fun searchQuestions(query: String): Flow<List<QuestionEntity>>

    /**
     * FTS4 全文搜索 — 支持中文分词，搜索 question + answer
     */
    @Query("""
        SELECT question_items.* FROM question_items
        JOIN question_items_fts ON question_items.rowid = question_items_fts.rowid
        WHERE question_items_fts MATCH :query
        ORDER BY question_items.created_at DESC
    """)
    fun searchQuestionsFts(query: String): Flow<List<QuestionEntity>>

    @Query("SELECT COUNT(*) FROM question_items")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>): List<Long>

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    @Query("UPDATE question_items SET answer = :answer, status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateAnswer(id: Long, answer: String?, status: String, updatedAt: Long = System.currentTimeMillis())
}
