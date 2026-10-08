package com.example.logapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.data.local.entity.DailyNoteEntity
import com.example.logapp.data.local.entity.SyncMetadataEntity
import com.example.logapp.data.local.dao.ActivityDao
import com.example.logapp.data.local.dao.SessionDao
import com.example.logapp.data.local.dao.DailyNoteDao

@Database(
    entities = [
        ActivityEntity::class,
        ActivitySessionEntity::class,
        DailyNoteEntity::class,
        SyncMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TimeLensDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun sessionDao(): SessionDao
    abstract fun dailyNoteDao(): DailyNoteDao
    abstract fun syncMetadataDao(): com.example.logapp.data.local.dao.SyncMetadataDao
}
