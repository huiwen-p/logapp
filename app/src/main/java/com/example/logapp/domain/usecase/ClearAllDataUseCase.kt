package com.example.logapp.domain.usecase

import android.content.Context
import android.content.Intent
import com.example.logapp.data.local.database.TimeLensDatabase
import com.example.logapp.service.notification.SessionNotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ClearAllDataUseCase @Inject constructor(
    private val database: TimeLensDatabase,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        // 1. Stop any running foreground notification timer
        try {
            val stopIntent = Intent(context, SessionNotificationService::class.java).apply {
                action = SessionNotificationService.ACTION_STOP
            }
            context.startService(stopIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Clear all tables in Room SQLite database
        database.clearAllTables()
    }
}
