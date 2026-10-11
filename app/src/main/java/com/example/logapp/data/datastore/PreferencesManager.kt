package com.example.logapp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    companion object {
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val BACKUP_KEEP_COUNT = intPreferencesKey("backup_keep_count")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val DAY_BOUNDARY = stringPreferencesKey("day_boundary") // "00:00", "04:00", etc.

        fun parseDayBoundary(boundary: String?): Pair<Int, Int> {
            if (boundary.isNullOrBlank()) return Pair(0, 0)
            val parts = boundary.split(":")
            if (parts.size >= 2) {
                val hours = (parts[0].trim().toIntOrNull() ?: 0).coerceIn(0, 23)
                val minutes = (parts[1].trim().toIntOrNull() ?: 0).coerceIn(0, 59)
                return Pair(hours, minutes)
            }
            val singleNumber = parts[0].trim().toIntOrNull()
            if (singleNumber != null) {
                return Pair(singleNumber.coerceIn(0, 23), 0)
            }
            return Pair(0, 0)
        }
    }

    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_BACKUP_ENABLED] ?: false
    }

    val backupFrequency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BACKUP_FREQUENCY] ?: "daily"
    }

    val backupKeepCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[BACKUP_KEEP_COUNT] ?: 7
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }

    val dayBoundary: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DAY_BOUNDARY] ?: "00:00"
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun setBackupFrequency(frequency: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKUP_FREQUENCY] = frequency
        }
    }

    suspend fun setBackupKeepCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[BACKUP_KEEP_COUNT] = count
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setDayBoundary(boundary: String) {
        context.dataStore.edit { preferences ->
            preferences[DAY_BOUNDARY] = boundary
        }
    }
}
