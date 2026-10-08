package com.example.logapp.core.di

import android.content.Context
import androidx.room.Room
import com.example.logapp.data.local.database.TimeLensDatabase
import com.example.logapp.data.local.dao.ActivityDao
import com.example.logapp.data.local.dao.DailyNoteDao
import com.example.logapp.data.local.dao.SessionDao
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.data.repository.ActivityRepositoryImpl
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.data.repository.SessionRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TimeLensDatabase {
        return Room.databaseBuilder(
            context,
            TimeLensDatabase::class.java,
            "timelens_db"
        ).build()
    }

    @Provides
    fun provideActivityDao(database: TimeLensDatabase): ActivityDao {
        return database.activityDao()
    }

    @Provides
    fun provideSessionDao(database: TimeLensDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    fun provideDailyNoteDao(database: TimeLensDatabase): DailyNoteDao {
        return database.dailyNoteDao()
    }

    @Provides
    @Singleton
    fun provideActivityRepository(activityDao: ActivityDao): ActivityRepository {
        return ActivityRepositoryImpl(activityDao)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(sessionDao: SessionDao): SessionRepository {
        return SessionRepositoryImpl(sessionDao)
    }

    @Provides
    fun provideSyncMetadataDao(database: TimeLensDatabase): com.example.logapp.data.local.dao.SyncMetadataDao {
        return database.syncMetadataDao()
    }

    @Provides
    @Singleton
    fun provideSyncMetadataRepository(dao: com.example.logapp.data.local.dao.SyncMetadataDao): com.example.logapp.domain.repository.SyncMetadataRepository {
        return com.example.logapp.data.repository.SyncMetadataRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideTransactionProvider(database: TimeLensDatabase): com.example.logapp.domain.repository.TransactionProvider {
        return com.example.logapp.data.repository.TransactionProviderImpl(database)
    }
}
