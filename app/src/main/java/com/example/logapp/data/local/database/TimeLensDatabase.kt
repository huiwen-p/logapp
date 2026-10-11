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
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TimeLensDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun sessionDao(): SessionDao
    abstract fun dailyNoteDao(): DailyNoteDao
    abstract fun syncMetadataDao(): com.example.logapp.data.local.dao.SyncMetadataDao
    abstract fun dataHealthDao(): com.example.logapp.data.local.dao.DataHealthDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_sessions_startedAt` ON `activity_sessions` (`startedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_sessions_endedAt` ON `activity_sessions` (`endedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_sessions_activityId` ON `activity_sessions` (`activityId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_sessions_deletedAt` ON `activity_sessions` (`deletedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_sessions_startedAt_endedAt` ON `activity_sessions` (`startedAt`, `endedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activities_isArchived` ON `activities` (`isArchived`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activities_sortOrder` ON `activities` (`sortOrder`)")
            }
        }
    }
}
