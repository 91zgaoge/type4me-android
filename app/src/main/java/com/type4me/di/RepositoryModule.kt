package com.type4me.di

import com.type4me.core.data.local.SettingsDataStore
import com.type4me.core.data.repository.ASRRepositoryImpl
import com.type4me.core.data.repository.LLMRepositoryImpl
import com.type4me.core.data.repository.SettingsRepositoryImpl
import com.type4me.core.domain.repository.ASRRepository
import com.type4me.core.domain.repository.LLMRepository
import com.type4me.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindASRRepository(
        impl: ASRRepositoryImpl
    ): ASRRepository

    @Binds
    @Singleton
    abstract fun bindLLMRepository(
        impl: LLMRepositoryImpl
    ): LLMRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): SettingsDataStore = SettingsDataStore(context)
}
