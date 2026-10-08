package com.example.logapp.service.notification

import android.content.Context
import android.content.Intent
import com.example.logapp.domain.controller.NotificationController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationController {

    override fun showActiveSessionNotification(sessionId: String, activityName: String, startedAt: Long) {
        val intent = Intent(context, SessionNotificationService::class.java).apply {
            action = SessionNotificationService.ACTION_START
            putExtra(SessionNotificationService.EXTRA_SESSION_ID, sessionId)
            putExtra(SessionNotificationService.EXTRA_ACTIVITY_NAME, activityName)
            putExtra(SessionNotificationService.EXTRA_STARTED_AT, startedAt)
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            // Foreground service starting can crash if app is in background in some Android versions.
            // Catch and handle gracefully.
            e.printStackTrace()
        }
    }

    override fun hideActiveSessionNotification() {
        val intent = Intent(context, SessionNotificationService::class.java).apply {
            action = SessionNotificationService.ACTION_STOP
        }
        context.startService(intent)
    }
}
