package com.example.logapp.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.logapp.data.datastore.PreferencesManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.example.logapp.domain.usecase.ExportFormat
import com.example.logapp.domain.usecase.ImportStrategy
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val dayBoundary by viewModel.dayBoundary.collectAsState()
    val isDriveSignedIn by viewModel.isDriveSignedIn.collectAsState()
    val driveBackups by viewModel.driveBackups.collectAsState()
    val isLoadingDriveBackups by viewModel.isLoadingDriveBackups.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val backupFrequency by viewModel.backupFrequency.collectAsState()
    val backupKeepCount by viewModel.backupKeepCount.collectAsState()
    val signedInEmail by viewModel.signedInEmail.collectAsState()
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val backupMessage by viewModel.backupMessage.collectAsState()
    val dataHealthReport by viewModel.dataHealthReport.collectAsState()
    val isCheckingHealth by viewModel.isCheckingHealth.collectAsState()
    val isRepairingHealth by viewModel.isRepairingHealth.collectAsState()
    var showDriveBackupsDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    val importPreviewData by viewModel.importPreviewData.collectAsState()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle result, update auth state
        viewModel.updateAuthState()
    }

    val createCsvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportData(context, ExportFormat.CSV, it) }
    }

    val createJsonDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportData(context, ExportFormat.JSON, it) }
    }

    val openJsonDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onJsonFileSelectedForImport(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            
            Column(Modifier.selectableGroup()) {
                listOf("system" to "System Default", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (value == themeMode),
                                onClick = { viewModel.setThemeMode(value) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (value == themeMode),
                            onClick = null
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Mốc bắt đầu ngày mới (Day Boundary)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Thời điểm chuyển sang ngày mới. Phù hợp cho người thức khuya qua đêm để các hoạt động trước mốc này vẫn được tính vào ngày hôm trước.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val (currentHour, currentMinute) = remember(dayBoundary) {
                PreferencesManager.parseDayBoundary(dayBoundary)
            }
            val formattedTime = String.format("%02d:%02d", currentHour, currentMinute)

            OutlinedCard(
                onClick = { showTimePickerDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bắt đầu ngày lúc",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FilledTonalButton(
                        onClick = { showTimePickerDialog = true }
                    ) {
                        Text("Thay đổi")
                    }
                }
            }

            Text(
                "Chọn nhanh mốc phổ biến:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "00:00" to "00:00 (Nửa đêm)",
                    "04:00" to "04:00 (Cú đêm)",
                    "05:00" to "05:00 (Dậy sớm)",
                    "06:00" to "06:00 (Sáng)"
                )
                presets.forEach { (time, label) ->
                    val isSelected = formattedTime == time
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setDayBoundary(time) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            HorizontalDivider()

            Text("Data Management", style = MaterialTheme.typography.titleMedium)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                        createCsvDocumentLauncher.launch("logapp_export_$timestamp.csv")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export CSV")
                }
                
                Button(
                    onClick = {
                        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                        createJsonDocumentLauncher.launch("logapp_export_$timestamp.json")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export JSON")
                }
            }
            
            Button(
                onClick = { openJsonDocumentLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import JSON")
            }

            HorizontalDivider()

            com.example.logapp.core.designsystem.component.LogAppSectionHeader(
                title = "Tình trạng dữ liệu (Data Health)",
                subtitle = "Quét và kiểm tra tính toàn vẹn của cơ sở dữ liệu",
                action = {
                    if (isCheckingHealth) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = { viewModel.checkDataHealth() }) {
                            Text("Kiểm tra")
                        }
                    }
                }
            )

            dataHealthReport?.let { report ->
                com.example.logapp.core.designsystem.component.LogAppOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Trạng thái cơ sở dữ liệu:",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (report.isHealthy) {
                                com.example.logapp.core.designsystem.component.LogAppStatusBadge(
                                    status = com.example.logapp.core.designsystem.component.StatusType.SUCCESS,
                                    text = "Lành mạnh (Healthy)"
                                )
                            } else {
                                com.example.logapp.core.designsystem.component.LogAppStatusBadge(
                                    status = com.example.logapp.core.designsystem.component.StatusType.WARNING,
                                    text = "Có lỗi cần xử lý"
                                )
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tổng số hoạt động:", style = MaterialTheme.typography.bodySmall)
                            Text("${report.totalActivities} hợp lệ", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tổng số phiên ghi nhận:", style = MaterialTheme.typography.bodySmall)
                            Text("${report.totalSessions} hợp lệ", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bản ghi mồ côi (Orphan):", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = if (report.orphanSessionsCount == 0) "✓ Không có" else "⚠ ${report.orphanSessionsCount} phiên",
                                fontWeight = FontWeight.SemiBold,
                                color = if (report.orphanSessionsCount == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Mốc thời gian (Timestamps):", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = if (report.invalidTimestampCount == 0) "✓ Nhất quán" else "⚠ ${report.invalidTimestampCount} phiên sai lệch",
                                fontWeight = FontWeight.SemiBold,
                                color = if (report.invalidTimestampCount == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (report.hasIssues) {
                            Spacer(modifier = Modifier.height(4.dp))
                            com.example.logapp.core.designsystem.component.LogAppDangerButton(
                                text = if (isRepairingHealth) "Đang tự động khắc phục..." else "Tự động khắc phục dữ liệu (Self-Healing)",
                                onClick = { viewModel.repairDataHealth() },
                                isLoading = isRepairingHealth
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text("Google Drive Backup", style = MaterialTheme.typography.titleMedium)
            
            if (isDriveSignedIn) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Đã kết nối với Google Drive", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    signedInEmail?.let { email ->
                        Text(
                            text = "Tài khoản: $email",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.triggerBackupNow(context) },
                        enabled = !isBackingUp,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đang lưu...")
                        } else {
                            Text("Sao lưu ngay")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.loadDriveBackups()
                            showDriveBackupsDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Khôi phục")
                    }
                }

                backupMessage?.let { msg ->
                    val isSuccess = msg.contains("thành công")
                    val isError = msg.contains("thất bại") || msg.contains("Lỗi")
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) MaterialTheme.colorScheme.primaryContainer 
                                else if (isError) MaterialTheme.colorScheme.errorContainer 
                                else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer 
                                else if (isError) MaterialTheme.colorScheme.onErrorContainer 
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // Auto Backup & Retention settings (SRS #37, #38)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Tự động sao lưu ngầm (Auto Backup)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Chạy qua WorkManager khi có Wi-Fi và pin không yếu",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = { viewModel.setAutoBackupEnabled(context, it) }
                            )
                        }

                        if (autoBackupEnabled) {
                            HorizontalDivider()

                            // Frequency selection (SRS #37)
                            Text(
                                "Tần suất sao lưu (Frequency):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val frequencies = listOf(
                                    "daily" to "Hàng ngày (Daily)",
                                    "weekly" to "Hàng tuần (Weekly)",
                                    "manual" to "Thủ công (Manual)"
                                )
                                frequencies.forEach { (freq, label) ->
                                    FilterChip(
                                        selected = backupFrequency.equals(freq, ignoreCase = true),
                                        onClick = { viewModel.setBackupFrequency(context, freq) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            // Retention / Rotation selection (SRS #38)
                            Text(
                                "Giới hạn số bản sao lưu (Rotation):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val keepOptions = listOf(
                                    7 to "7 bản gần nhất (Mặc định)",
                                    30 to "30 bản gần nhất",
                                    1 to "Chỉ 1 bản mới nhất",
                                    Int.MAX_VALUE to "Lưu tất cả"
                                )
                                keepOptions.forEach { (count, label) ->
                                    FilterChip(
                                        selected = backupKeepCount == count,
                                        onClick = { viewModel.setBackupKeepCount(count) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.signOutDrive(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đăng xuất khỏi Google Drive")
                }
            } else {
                Button(
                    onClick = { 
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect to Google Drive")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDriveBackupsDialog) {
        AlertDialog(
            onDismissRequest = { showDriveBackupsDialog = false },
            title = { Text("Select Backup to Restore") },
            text = {
                if (isLoadingDriveBackups) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (driveBackups.isEmpty()) {
                    Text("No backups found on Google Drive.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(driveBackups) { file ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onDriveBackupSelectedForImport(file.id)
                                        showDriveBackupsDialog = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (file.createdTime > 0) {
                                        val formattedDate = java.time.Instant.ofEpochMilli(file.createdTime)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDriveBackupsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    importPreviewData?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.clearImportPreview() },
            title = { Text("Import Data") },
            text = { 
                Text("Found ${preview.activities.size} activities and ${preview.sessions.size} sessions.\n\nMerge: Add missing records.\nReplace: Delete existing data and use the import file.") 
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport(ImportStrategy.MERGE) }) {
                    Text("Merge")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.confirmImport(ImportStrategy.REPLACE) }) {
                        Text("Replace")
                    }
                    TextButton(onClick = { viewModel.clearImportPreview() }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showTimePickerDialog) {
        val (currentHour, currentMinute) = PreferencesManager.parseDayBoundary(dayBoundary)
        DayBoundaryTimePickerDialog(
            initialHour = currentHour,
            initialMinute = currentMinute,
            onConfirm = { hour, minute ->
                val formatted = String.format("%02d:%02d", hour, minute)
                viewModel.setDayBoundary(formatted)
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayBoundaryTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn mốc bắt đầu ngày mới") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(state.hour, state.minute)
            }) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
