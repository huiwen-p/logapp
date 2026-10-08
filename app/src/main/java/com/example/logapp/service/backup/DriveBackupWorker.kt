package com.example.logapp.service.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.logapp.domain.usecase.DriveBackupUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DriveBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val driveBackupUseCase: DriveBackupUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val result = driveBackupUseCase()
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
