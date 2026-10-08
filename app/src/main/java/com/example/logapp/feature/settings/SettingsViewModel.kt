package com.example.logapp.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logapp.data.datastore.PreferencesManager
import com.example.logapp.domain.repository.GoogleAuthRepository
import com.example.logapp.service.backup.DriveBackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val googleAuthRepository: GoogleAuthRepository
) : ViewModel() {

    val themeMode: StateFlow<String> = preferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val dayBoundary: StateFlow<String> = preferencesManager.dayBoundary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00")

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setDayBoundary(boundary: String) {
        viewModelScope.launch {
            preferencesManager.setDayBoundary(boundary)
        }
    }

    val isDriveSignedIn = MutableStateFlow(googleAuthRepository.isSignedIn())

    fun updateAuthState() {
        isDriveSignedIn.value = googleAuthRepository.isSignedIn()
    }

    fun signOutDrive() {
        googleAuthRepository.signOut()
        updateAuthState()
    }

    fun triggerBackupNow(context: Context) {
        DriveBackupScheduler.scheduleBackupNow(context)
    }

    fun setPeriodicBackupEnabled(context: Context, enabled: Boolean) {
        if (enabled) {
            DriveBackupScheduler.enablePeriodicBackup(context)
        } else {
            DriveBackupScheduler.disablePeriodicBackup(context)
        }
    }
}
