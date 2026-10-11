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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import com.example.logapp.domain.model.backup.BackupSchemaV1
import com.example.logapp.domain.usecase.ExportDataUseCase
import com.example.logapp.domain.usecase.ExportFormat
import com.example.logapp.domain.usecase.ImportDataUseCase
import com.example.logapp.domain.usecase.ImportStrategy
import java.io.OutputStreamWriter
import javax.inject.Inject

import com.example.logapp.domain.backup.DriveBackupManager
import com.example.logapp.domain.backup.DriveBackupFile
import com.example.logapp.domain.usecase.DriveBackupUseCase
import com.example.logapp.domain.model.health.DataHealthReport
import com.example.logapp.domain.usecase.CheckDataHealthUseCase
import com.example.logapp.domain.usecase.RepairDataHealthUseCase
import android.util.Log

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val googleAuthRepository: GoogleAuthRepository,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val driveBackupManager: DriveBackupManager,
    private val driveBackupUseCase: DriveBackupUseCase,
    private val checkDataHealthUseCase: CheckDataHealthUseCase,
    private val repairDataHealthUseCase: RepairDataHealthUseCase
) : ViewModel() {

    val dataHealthReport = MutableStateFlow<DataHealthReport?>(null)
    val isCheckingHealth = MutableStateFlow(false)
    val isRepairingHealth = MutableStateFlow(false)

    init {
        checkDataHealth()
    }

    fun checkDataHealth() {
        viewModelScope.launch {
            isCheckingHealth.value = true
            try {
                dataHealthReport.value = checkDataHealthUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isCheckingHealth.value = false
            }
        }
    }

    fun repairDataHealth() {
        viewModelScope.launch {
            isRepairingHealth.value = true
            try {
                dataHealthReport.value = repairDataHealthUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRepairingHealth.value = false
            }
        }
    }

    val themeMode: StateFlow<String> = preferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val dayBoundary: StateFlow<String> = preferencesManager.dayBoundary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00")

    val autoBackupEnabled: StateFlow<Boolean> = preferencesManager.autoBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val backupFrequency: StateFlow<String> = preferencesManager.backupFrequency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "daily")

    val backupKeepCount: StateFlow<Int> = preferencesManager.backupKeepCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)

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
    val signedInEmail = MutableStateFlow(googleAuthRepository.getSignedInEmail())
    val isBackingUp = MutableStateFlow(false)
    val backupMessage = MutableStateFlow<String?>(null)

    fun updateAuthState() {
        val signedIn = googleAuthRepository.isSignedIn()
        isDriveSignedIn.value = signedIn
        signedInEmail.value = if (signedIn) googleAuthRepository.getSignedInEmail() else null
    }

    fun signOutDrive(context: Context? = null) {
        googleAuthRepository.signOut()
        updateAuthState()
        context?.let { DriveBackupScheduler.disablePeriodicBackup(it) }
        backupMessage.value = null
    }

    fun triggerBackupNow(context: Context? = null) {
        viewModelScope.launch {
            isBackingUp.value = true
            backupMessage.value = "Đang sao lưu lên Google Drive..."
            try {
                val result = driveBackupUseCase()
                result.onSuccess {
                    backupMessage.value = "Sao lưu lên Google Drive thành công!"
                    loadDriveBackups()
                }.onFailure { e ->
                    Log.e("DriveBackup", "Manual backup failed", e)
                    val rawMsg = e.localizedMessage ?: e.message ?: "Lỗi không xác định"
                    val displayMsg = if (rawMsg.contains("403") || rawMsg.contains("disabled") || rawMsg.contains("accessNotConfigured")) {
                        "Lỗi 403: Google Drive API chưa được kích hoạt trên Google Cloud Console cho project. Vui lòng vào console.cloud.google.com bật Drive API."
                    } else {
                        "Sao lưu thất bại: $rawMsg"
                    }
                    backupMessage.value = displayMsg
                }
            } catch (e: Exception) {
                Log.e("DriveBackup", "Manual backup exception", e)
                backupMessage.value = "Sao lưu thất bại: ${e.message}"
            } finally {
                isBackingUp.value = false
            }
        }
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoBackupEnabled(enabled)
            if (enabled) {
                val freq = preferencesManager.backupFrequency.first()
                DriveBackupScheduler.schedulePeriodicBackup(context, freq)
            } else {
                DriveBackupScheduler.disablePeriodicBackup(context)
            }
        }
    }

    fun setBackupFrequency(context: Context, frequency: String) {
        viewModelScope.launch {
            preferencesManager.setBackupFrequency(frequency)
            val isEnabled = preferencesManager.autoBackupEnabled.first()
            if (isEnabled) {
                DriveBackupScheduler.schedulePeriodicBackup(context, frequency)
            }
        }
    }

    fun setBackupKeepCount(keepCount: Int) {
        viewModelScope.launch {
            preferencesManager.setBackupKeepCount(keepCount)
        }
    }

    fun setPeriodicBackupEnabled(context: Context, enabled: Boolean) {
        setAutoBackupEnabled(context, enabled)
    }

    fun exportData(context: Context, format: ExportFormat, uri: Uri) {
        viewModelScope.launch {
            try {
                val data = exportDataUseCase(format)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                            writer.write(data)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Ideally post an error state or event here
            }
        }
    }

    val importPreviewData = MutableStateFlow<BackupSchemaV1?>(null)
    private var pendingImportJson: String? = null

    fun onJsonFileSelectedForImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }
                }
                
                if (jsonString != null) {
                    val preview = importDataUseCase.previewImport(jsonString)
                    pendingImportJson = jsonString
                    importPreviewData.value = preview
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun confirmImport(strategy: ImportStrategy) {
        val json = pendingImportJson ?: return
        viewModelScope.launch {
            try {
                importDataUseCase(json, strategy)
                clearImportPreview()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearImportPreview() {
        importPreviewData.value = null
        pendingImportJson = null
    }

    val driveBackups = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val isLoadingDriveBackups = MutableStateFlow(false)

    fun loadDriveBackups() {
        viewModelScope.launch {
            isLoadingDriveBackups.value = true
            driveBackupManager.listBackups()
                .onSuccess { backups ->
                    driveBackups.value = backups
                }
                .onFailure { e ->
                    e.printStackTrace()
                    driveBackups.value = emptyList()
                }
            isLoadingDriveBackups.value = false
        }
    }

    fun onDriveBackupSelectedForImport(fileId: String) {
        viewModelScope.launch {
            driveBackupManager.downloadBackup(fileId)
                .onSuccess { jsonString ->
                    try {
                        val preview = importDataUseCase.previewImport(jsonString)
                        pendingImportJson = jsonString
                        importPreviewData.value = preview
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .onFailure { e ->
                    e.printStackTrace()
                }
        }
    }
}
