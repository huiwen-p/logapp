package com.example.logapp.feature.settings.model

sealed interface CloudBackupState {
    object Disconnected : CloudBackupState
    data class Connected(
        val email: String,
        val lastBackupTimeFormatted: String?,
        val isAutoBackupEnabled: Boolean,
        val frequency: String
    ) : CloudBackupState
    object BackingUp : CloudBackupState
    data class Success(val message: String, val lastBackupTimeFormatted: String) : CloudBackupState
    data class Error(val message: String) : CloudBackupState
}

sealed interface CloudSyncState {
    data class LocalOnly(val pendingChangesCount: Int) : CloudSyncState
    data class Syncing(val pendingChangesCount: Int) : CloudSyncState
    data class Synced(val lastSyncedTimeFormatted: String?) : CloudSyncState
    data class Error(val message: String, val pendingChangesCount: Int) : CloudSyncState
}
