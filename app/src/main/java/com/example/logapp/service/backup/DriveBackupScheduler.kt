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

    fun enablePeriodicBackup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Typically we only backup automatically on Wi-Fi
            .setRequiresCharging(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<DriveBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    fun disablePeriodicBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_BACKUP_WORK_NAME)
    }
}
