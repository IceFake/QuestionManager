package com.example.questionmanager.di

import com.example.questionmanager.data.local.datastore.SecureDataStore
import com.example.questionmanager.data.local.datastore.SettingsDataStore
import com.example.questionmanager.data.local.db.dao.PromptDao
import com.example.questionmanager.data.local.db.dao.QuestionDao
import com.example.questionmanager.data.local.db.dao.QuestionLinkDao
import com.example.questionmanager.data.remote.api.DeepSeekApiService
import com.example.questionmanager.data.remote.api.WebParserService
import com.example.questionmanager.data.repository.AiRepository
import com.example.questionmanager.data.repository.QuestionRepository
import com.example.questionmanager.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideQuestionRepository(
        questionDao: QuestionDao,
        questionLinkDao: QuestionLinkDao,
        promptDao: PromptDao
    ): QuestionRepository {
        return QuestionRepository(questionDao, questionLinkDao, promptDao)
    }

    @Provides
    @Singleton
    fun provideAiRepository(
        deepSeekApiService: DeepSeekApiService,
        webParserService: WebParserService,
        settingsRepository: SettingsRepository
    ): AiRepository {
        return AiRepository(deepSeekApiService, webParserService, settingsRepository)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsDataStore: SettingsDataStore,
        secureDataStore: SecureDataStore
    ): SettingsRepository {
        return SettingsRepository(settingsDataStore, secureDataStore)
    }
}

