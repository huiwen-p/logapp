package com.example.logapp.data.local.dao

import androidx.room.*
import com.example.logapp.data.local.entity.DailyNoteEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyNoteDao {
    @Query("SELECT * FROM daily_notes WHERE date = :date")
    fun getNoteByDate(date: LocalDate): Flow<DailyNoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: DailyNoteEntity)

    @Update
    suspend fun update(note: DailyNoteEntity)
}
