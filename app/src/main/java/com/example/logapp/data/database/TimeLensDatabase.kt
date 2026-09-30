package com.example.logapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.logapp.data.model.ActivityEntity
import com.example.logapp.data.model.ActivitySessionEntity
import com.example.logapp.data.model.DailyNoteEntity
import com.example.logapp.data.model.SyncMetadataEntity
import com.example.logapp.data.database.dao.ActivityDao
import com.example.logapp.data.database.dao.SessionDao
import com.example.logapp.data.database.dao.DailyNoteDao

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
}
