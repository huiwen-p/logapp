package com.example.logapp.core.di

import com.example.logapp.data.remote.AuthRepositoryImpl
import com.example.logapp.data.remote.FirestoreRepositoryImpl
import com.example.logapp.domain.repository.AuthRepository
import com.example.logapp.domain.repository.FirestoreRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFirestoreRepository(impl: FirestoreRepositoryImpl): FirestoreRepository
}
