package com.example.logapp.service.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object DriveBackupScheduler {
    private const val PERIODIC_BACKUP_WORK_NAME = "DrivePeriodicBackup"

    fun scheduleBackupNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val backupWorkRequest = OneTimeWorkRequestBuilder<DriveBackupWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(backupWorkRequest)
    }

    fun schedulePeriodicBackup(context: Context, frequency: String) {
        if (frequency.equals("manual", ignoreCase = true)) {
            disablePeriodicBackup(context)
            return
        }

        val repeatIntervalDays = when (frequency.lowercase()) {
            "weekly" -> 7L
            else -> 1L // "daily" or default
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Typically auto backup on Wi-Fi
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<DriveBackupWorker>(repeatIntervalDays, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }

    fun enablePeriodicBackup(context: Context) {
        schedulePeriodicBackup(context, "daily")
    }

    fun disablePeriodicBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_BACKUP_WORK_NAME)
    }
}
