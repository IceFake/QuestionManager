package com.example.questionmanager.di

import android.content.Context
import androidx.room.Room
import com.example.questionmanager.data.local.datastore.SecureDataStore
import com.example.questionmanager.data.local.datastore.SettingsDataStore
import com.example.questionmanager.data.local.db.AppDatabase
import com.example.questionmanager.data.local.db.dao.PromptDao
import com.example.questionmanager.data.local.db.dao.QuestionDao
import com.example.questionmanager.data.local.db.dao.QuestionLinkDao
import com.example.questionmanager.data.local.db.DatabaseCallback
import com.example.questionmanager.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        lateinit var database: AppDatabase
        database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .addCallback(DatabaseCallback { database.promptDao() })
            .fallbackToDestructiveMigration(dropAllTables = true) // ⚠️ 仅限开发阶段，正式版本必须移除
            .build()
        return database
    }

    @Provides
    @Singleton
    fun provideQuestionDao(database: AppDatabase): QuestionDao {
        return database.questionDao()
    }

    @Provides
    @Singleton
    fun provideQuestionLinkDao(database: AppDatabase): QuestionLinkDao {
        return database.questionLinkDao()
    }

    @Provides
    @Singleton
    fun providePromptDao(database: AppDatabase): PromptDao {
        return database.promptDao()
    }

    @Provides
    @Singleton
    fun provideSecureDataStore(@ApplicationContext context: Context): SecureDataStore {
        return SecureDataStore(context)
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
}

