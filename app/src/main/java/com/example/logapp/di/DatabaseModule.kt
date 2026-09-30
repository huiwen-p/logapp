package com.example.logapp.di

import android.content.Context
import androidx.room.Room
import com.example.logapp.data.database.TimeLensDatabase
import com.example.logapp.data.database.dao.ActivityDao
import com.example.logapp.data.database.dao.DailyNoteDao
import com.example.logapp.data.database.dao.SessionDao
import com.example.logapp.data.repository.ActivityRepository
import com.example.logapp.data.repository.ActivityRepositoryImpl
import com.example.logapp.data.repository.SessionRepository
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
}
