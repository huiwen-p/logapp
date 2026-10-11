package com.example.logapp.feature.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logapp.data.datastore.PreferencesManager
import com.example.logapp.domain.backup.DriveBackupFile
import com.example.logapp.domain.backup.DriveBackupManager
import com.example.logapp.domain.model.backup.BackupSchemaV1
import com.example.logapp.domain.model.health.DataHealthReport
import com.example.logapp.domain.repository.GoogleAuthRepository
import com.example.logapp.domain.repository.SyncMetadataRepository
import com.example.logapp.domain.usecase.*
import com.example.logapp.feature.settings.model.CloudBackupState
import com.example.logapp.feature.settings.model.CloudSyncState
import com.example.logapp.service.backup.DriveBackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val googleAuthRepository: GoogleAuthRepository,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val driveBackupManager: DriveBackupManager,
    private val driveBackupUseCase: DriveBackupUseCase,
    private val checkDataHealthUseCase: CheckDataHealthUseCase,
    private val repairDataHealthUseCase: RepairDataHealthUseCase,
    private val syncMetadataRepository: SyncMetadataRepository
) : ViewModel() {

    val dataHealthReport = MutableStateFlow<DataHealthReport?>(null)
    val isCheckingHealth = MutableStateFlow(false)
    val isRepairingHealth = MutableStateFlow(false)

    val userMessage = MutableStateFlow<String?>(null)
    val isExporting = MutableStateFlow(false)
    val isImporting = MutableStateFlow(false)

    val pendingSyncCount = MutableStateFlow(0)

    val lastBackupTime: StateFlow<String?> = preferencesManager.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastBackupTimeFormatted: StateFlow<String?> = lastBackupTime
        .map { formatBackupTime(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    val isDriveSignedIn = MutableStateFlow(googleAuthRepository.isSignedIn())
    val signedInEmail = MutableStateFlow(googleAuthRepository.getSignedInEmail())
    val isBackingUp = MutableStateFlow(false)
    val backupMessage = MutableStateFlow<String?>(null)

    val importPreviewData = MutableStateFlow<BackupSchemaV1?>(null)
    private var pendingImportJson: String? = null

    val driveBackups = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val isLoadingDriveBackups = MutableStateFlow(false)

    init {
        checkDataHealth()
        refreshPendingSyncs()
    }

    fun clearUserMessage() {
        userMessage.value = null
    }

    fun refreshPendingSyncs() {
        viewModelScope.launch {
            try {
                pendingSyncCount.value = syncMetadataRepository.getPendingSyncs().size
            } catch (e: Exception) {
                pendingSyncCount.value = 0
            }
        }
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
                userMessage.value = "Đã tự động khắc phục các bất thường dữ liệu!"
            } catch (e: Exception) {
                e.printStackTrace()
                userMessage.value = "Lỗi khi tự sửa lỗi dữ liệu: ${e.message}"
            } finally {
                isRepairingHealth.value = false
            }
        }
    }

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
        userMessage.value = "Đã ngắt kết nối Google Drive"
    }

    fun triggerBackupNow(context: Context? = null) {
        viewModelScope.launch {
            isBackingUp.value = true
            backupMessage.value = "Đang sao lưu lên Google Drive..."
            try {
                val result = driveBackupUseCase()
                result.onSuccess {
                    val nowIso = Instant.now().toString()
                    preferencesManager.setLastBackupTime(nowIso)
                    backupMessage.value = "Sao lưu lên Google Drive thành công!"
                    userMessage.value = "Sao lưu lên Google Drive thành công!"
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
                    userMessage.value = displayMsg
                }
            } catch (e: Exception) {
                Log.e("DriveBackup", "Manual backup exception", e)
                backupMessage.value = "Sao lưu thất bại: ${e.message}"
                userMessage.value = "Sao lưu thất bại: ${e.message}"
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

    fun exportData(context: Context, format: ExportFormat, uri: Uri) {
        viewModelScope.launch {
            isExporting.value = true
            try {
                val data = exportDataUseCase(format)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                            writer.write(data)
                        }
                    }
                }
                userMessage.value = "Đã xuất dữ liệu ${format.name} thành công!"
            } catch (e: Exception) {
                e.printStackTrace()
                userMessage.value = "Lỗi khi xuất dữ liệu: ${e.message}"
            } finally {
                isExporting.value = false
            }
        }
    }

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
                userMessage.value = "Tệp JSON không hợp lệ hoặc sai định dạng: ${e.message}"
            }
        }
    }

    fun confirmImport(strategy: ImportStrategy) {
        val json = pendingImportJson ?: return
        val preview = importPreviewData.value
        viewModelScope.launch {
            isImporting.value = true
            try {
                importDataUseCase(json, strategy)
                val actCount = preview?.activities?.size ?: 0
                val sesCount = preview?.sessions?.size ?: 0
                userMessage.value = "Đã nhập thành công $actCount hoạt động và $sesCount phiên ghi nhận!"
                clearImportPreview()
                checkDataHealth()
                refreshPendingSyncs()
            } catch (e: Exception) {
                e.printStackTrace()
                userMessage.value = "Lỗi khi nhập dữ liệu: ${e.message}"
            } finally {
                isImporting.value = false
            }
        }
    }

    fun clearImportPreview() {
        importPreviewData.value = null
        pendingImportJson = null
    }

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
                        userMessage.value = "Tệp sao lưu trên Drive không hợp lệ: ${e.message}"
                    }
                }
                .onFailure { e ->
                    e.printStackTrace()
                    userMessage.value = "Lỗi khi tải bản sao lưu từ Drive: ${e.message}"
                }
        }
    }

    private fun formatBackupTime(isoString: String?): String? {
        if (isoString.isNullOrBlank()) return null
        return try {
            val instant = Instant.parse(isoString)
            val zone = ZoneId.systemDefault()
            val zonedDateTime = instant.atZone(zone)
            val today = LocalDate.now()
            val backupDate = zonedDateTime.toLocalDate()
            val timePart = zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            if (backupDate == today) {
                "Hôm nay $timePart"
            } else if (backupDate == today.minusDays(1)) {
                "Hôm qua $timePart"
            } else {
                zonedDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            }
        } catch (e: Exception) {
            isoString
        }
    }
}
