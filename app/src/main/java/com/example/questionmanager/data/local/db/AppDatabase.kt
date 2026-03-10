package com.example.questionmanager.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.questionmanager.data.local.db.dao.PromptDao
import com.example.questionmanager.data.local.db.dao.QuestionDao
import com.example.questionmanager.data.local.db.dao.QuestionLinkDao
import com.example.questionmanager.data.local.db.entity.PromptEntity
import com.example.questionmanager.data.local.db.entity.QuestionEntity
import com.example.questionmanager.data.local.db.entity.QuestionFtsEntity
import com.example.questionmanager.data.local.db.entity.QuestionLinkEntity

@Database(
    entities = [
        QuestionEntity::class,
        QuestionLinkEntity::class,
        PromptEntity::class,
        QuestionFtsEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun questionLinkDao(): QuestionLinkDao
    abstract fun promptDao(): PromptDao
}

