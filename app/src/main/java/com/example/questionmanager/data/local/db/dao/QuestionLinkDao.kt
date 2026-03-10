package com.example.questionmanager.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.questionmanager.data.local.db.entity.QuestionEntity
import com.example.questionmanager.data.local.db.entity.QuestionLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionLinkDao {
    @Query("SELECT * FROM question_links WHERE parent_id = :parentId")
    fun getChildLinks(parentId: Long): Flow<List<QuestionLinkEntity>>

    @Query("SELECT * FROM question_links WHERE child_id = :childId")
    fun getParentLinks(childId: Long): Flow<List<QuestionLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: QuestionLinkEntity): Long

    @Delete
    suspend fun deleteLink(link: QuestionLinkEntity)

    @Query("DELETE FROM question_links WHERE parent_id = :parentId AND child_id = :childId")
    suspend fun deleteLinkByParentAndChild(parentId: Long, childId: Long)

    @Query("""
        SELECT qi.* FROM question_items qi 
        INNER JOIN question_links ql ON qi.id = ql.child_id 
        WHERE ql.parent_id = :parentId
    """)
    fun getLinkedChildQuestions(parentId: Long): Flow<List<QuestionEntity>>

    @Query("""
        SELECT qi.* FROM question_items qi 
        INNER JOIN question_links ql ON qi.id = ql.parent_id 
        WHERE ql.child_id = :childId
    """)
    fun getLinkedParentQuestions(childId: Long): Flow<List<QuestionEntity>>
}
