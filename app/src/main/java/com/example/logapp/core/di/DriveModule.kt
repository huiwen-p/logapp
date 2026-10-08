package com.example.logapp.core.di

import com.example.logapp.data.backup.DriveBackupManagerImpl
import com.example.logapp.data.remote.GoogleAuthRepositoryImpl
import com.example.logapp.domain.backup.DriveBackupManager
import com.example.logapp.domain.repository.GoogleAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DriveModule {

    @Binds
    @Singleton
    abstract fun bindGoogleAuthRepository(impl: GoogleAuthRepositoryImpl): GoogleAuthRepository

    @Binds
    @Singleton
    abstract fun bindDriveBackupManager(impl: DriveBackupManagerImpl): DriveBackupManager
}
