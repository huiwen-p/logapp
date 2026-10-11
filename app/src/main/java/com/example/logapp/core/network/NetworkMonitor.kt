package com.example.logapp.core.network

import kotlinx.coroutines.flow.Flow

/**
 * Utility for monitoring network connectivity status across the app.
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
