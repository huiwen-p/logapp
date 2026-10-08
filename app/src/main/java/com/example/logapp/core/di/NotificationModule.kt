package com.example.logapp.core.di

import com.example.logapp.domain.controller.NotificationController
import com.example.logapp.service.notification.NotificationControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationController(impl: NotificationControllerImpl): NotificationController
}
