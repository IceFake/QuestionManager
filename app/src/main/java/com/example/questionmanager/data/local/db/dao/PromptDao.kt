package com.example.questionmanager.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.questionmanager.data.local.db.entity.PromptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts ORDER BY created_at DESC")
    fun getAllPrompts(): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultPrompt(): PromptEntity?

    @Query("SELECT * FROM prompts WHERE id = :id")
    suspend fun getPromptById(id: Long): PromptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: PromptEntity): Long

    @Update
    suspend fun updatePrompt(prompt: PromptEntity)

    @Delete
    suspend fun deletePrompt(prompt: PromptEntity)

    @Query("UPDATE prompts SET is_default = 0")
    suspend fun clearDefaultPrompt()
}
