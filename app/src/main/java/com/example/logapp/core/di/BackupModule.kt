package com.example.logapp.core.di

import com.example.logapp.data.backup.BackupSerializerImpl
import com.example.logapp.domain.backup.BackupSerializer
import com.example.logapp.domain.backup.CsvGenerator
import com.example.logapp.domain.backup.ImportValidationEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    fun provideBackupSerializer(): BackupSerializer {
        return BackupSerializerImpl()
    }

    @Provides
    @Singleton
    fun provideCsvGenerator(): CsvGenerator {
        return CsvGenerator()
    }

    @Provides
    @Singleton
    fun provideImportValidationEngine(): ImportValidationEngine {
        return ImportValidationEngine()
    }
}
