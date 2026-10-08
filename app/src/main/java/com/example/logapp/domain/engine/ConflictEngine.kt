package com.example.logapp.domain.engine

enum class ConflictResolution {
    KEEP_LOCAL,
    USE_REMOTE
}

object ConflictEngine {
    fun resolveConflict(localVersion: Int, remoteVersion: Int): ConflictResolution {
        // Last-write-wins based on version.
        // If remote version is strictly greater, use remote.
        // If they are equal or local is greater, keep local.
        return if (remoteVersion > localVersion) {
            ConflictResolution.USE_REMOTE
        } else {
            ConflictResolution.KEEP_LOCAL
        }
    }
}
