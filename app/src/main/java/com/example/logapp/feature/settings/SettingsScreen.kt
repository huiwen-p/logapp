package com.example.logapp.feature.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logapp.core.designsystem.component.*
import com.example.logapp.core.designsystem.tokens.spacing
import com.example.logapp.data.datastore.PreferencesManager
import com.example.logapp.domain.usecase.ExportFormat
import com.example.logapp.domain.usecase.ImportStrategy
import com.example.logapp.feature.settings.components.ImportPreviewDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
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
    val lastBackupTimeFormatted by viewModel.lastBackupTimeFormatted.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val dataHealthReport by viewModel.dataHealthReport.collectAsState()
    val isCheckingHealth by viewModel.isCheckingHealth.collectAsState()
    val isRepairingHealth by viewModel.isRepairingHealth.collectAsState()

    var showDriveBackupsDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    val importPreviewData by viewModel.importPreviewData.collectAsState()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt & Dữ liệu", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.medium, vertical = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            // ==========================================
            // 1. DATA SAFETY & CLOUD SYNC STATE (Phase 6)
            // ==========================================
            LogAppSectionHeader(
                title = "ĐỒNG BỘ & SAO LƯU ĐÁM MÂY",
                subtitle = "Trực quan hóa trạng thái an toàn dữ liệu cá nhân"
            )

            // Firebase Cloud Sync Status Card
            LogAppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(
                            text = "Đồng bộ đám mây (Cloud Sync)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LogAppStatusBadge(
                        status = StatusType.NEUTRAL,
                        text = "Chế độ Cục bộ (Local Only)"
                    )
                }

                Spacer(modifier = Modifier.height(spacing.small))

                Text(
                    text = "Dữ liệu được lưu trữ an toàn 100% trên thiết bị (SQLite/Room). Khi bạn liên kết tài khoản đám mây, hệ thống sẽ tự động đồng bộ 2 chiều mà không làm gián đoạn trải nghiệm ngoại tuyến.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.small))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thay đổi cục bộ sẵn sàng đồng bộ:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$pendingSyncCount bản ghi",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingSyncCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Google Drive Backup Card
            LogAppElevatedCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDriveSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(
                            text = "Google Drive Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LogAppStatusBadge(
                        status = if (isDriveSignedIn) StatusType.SUCCESS else StatusType.NEUTRAL,
                        text = if (isDriveSignedIn) "Đã kết nối" else "Chưa liên kết"
                    )
                }

                Spacer(modifier = Modifier.height(spacing.small))

                if (isDriveSignedIn) {
                    signedInEmail?.let { email ->
                        Text(
                            text = "Tài khoản liên kết: $email",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.extraSmall))

                    // Last Backup Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Bản sao lưu gần nhất: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lastBackupTimeFormatted ?: "Chưa có bản sao lưu nào",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (lastBackupTimeFormatted != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.medium))

                    // Backup Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        LogAppPrimaryButton(
                            text = "Sao lưu ngay",
                            onClick = { viewModel.triggerBackupNow(context) },
                            isLoading = isBackingUp,
                            modifier = Modifier.weight(1f)
                        )

                        LogAppSecondaryButton(
                            text = "Khôi phục",
                            onClick = {
                                viewModel.loadDriveBackups()
                                showDriveBackupsDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    backupMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(spacing.small))
                        val isSuccess = msg.contains("thành công")
                        val isError = msg.contains("thất bại") || msg.contains("Lỗi")
                        Surface(
                            color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer
                                    else if (isError) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(spacing.small)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.medium))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(spacing.small))

                    // Auto Backup Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tự động sao lưu định kỳ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Chạy ngầm qua WorkManager khi có Wi-Fi và pin không yếu",
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
                        Spacer(modifier = Modifier.height(spacing.small))
                        Text(
                            text = "Tần suất sao lưu:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            listOf("daily" to "Hàng ngày (Daily)", "weekly" to "Hàng tuần (Weekly)", "manual" to "Thủ công (Manual)").forEach { (freq, label) ->
                                FilterChip(
                                    selected = backupFrequency.equals(freq, ignoreCase = true),
                                    onClick = { viewModel.setBackupFrequency(context, freq) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.small))
                        Text(
                            text = "Giới hạn số bản sao lưu:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            listOf(
                                7 to "7 bản (Mặc định)",
                                30 to "30 bản",
                                1 to "Chỉ 1 bản mới nhất"
                            ).forEach { (count, label) ->
                                FilterChip(
                                    selected = backupKeepCount == count,
                                    onClick = { viewModel.setBackupKeepCount(count) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.small))
                    TextButton(
                        onClick = { viewModel.signOutDrive(context) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Ngắt kết nối Google Drive")
                    }
                } else {
                    Text(
                        text = "Liên kết tài khoản Google Drive để ứng dụng tự động sao lưu định kỳ các bản ghi lên đám mây cá nhân của bạn, hỗ trợ khôi phục khi chuyển thiết bị.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))
                    LogAppSecondaryButton(
                        text = "Liên kết tài khoản Google Drive",
                        icon = Icons.Default.AccountCircle,
                        onClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(client.signInIntent)
                        }
                    )
                }
            }

            // ==========================================
            // 2. LOCAL EXPORT & IMPORT (OFFLINE-FIRST)
            // ==========================================
            LogAppSectionHeader(
                title = "SAO LƯU & XUẤT DỮ LIỆU CỤC BỘ",
                subtitle = "Xuất và nhập tệp tin lưu trực tiếp trên thiết bị (hoạt động 100% offline)"
            )

            LogAppCard {
                Text(
                    text = "Tệp sao lưu độc lập (JSON/CSV) giúp bạn sở hữu trọn vẹn dữ liệu cá nhân, dễ dàng chia sẻ hoặc lưu trữ an toàn trong máy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    LogAppSecondaryButton(
                        text = "Xuất JSON",
                        icon = Icons.Default.Share,
                        isLoading = isExporting,
                        onClick = {
                            val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                            createJsonDocumentLauncher.launch("timelens_backup_$timestamp.json")
                        },
                        modifier = Modifier.weight(1f)
                    )

                    LogAppSecondaryButton(
                        text = "Xuất CSV",
                        icon = Icons.Default.List,
                        isLoading = isExporting,
                        onClick = {
                            val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                            createCsvDocumentLauncher.launch("timelens_sessions_$timestamp.csv")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(spacing.small))

                LogAppPrimaryButton(
                    text = "Khôi phục từ tệp JSON...",
                    icon = Icons.Default.Add,
                    isLoading = isImporting,
                    onClick = { openJsonDocumentLauncher.launch(arrayOf("application/json")) }
                )
            }

            // ==========================================
            // 3. DATA HEALTH & SELF-HEALING
            // ==========================================
            LogAppSectionHeader(
                title = "TÌNH TRẠNG CƠ SỞ DỮ LIỆU (DATA HEALTH)",
                subtitle = "Quét và kiểm tra tính toàn vẹn của dữ liệu cục bộ",
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
                LogAppCard {
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
                            LogAppStatusBadge(
                                status = StatusType.SUCCESS,
                                text = "Lành mạnh (Healthy)"
                            )
                        } else {
                            LogAppStatusBadge(
                                status = StatusType.WARNING,
                                text = "Có bất thường"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.small))
                    HorizontalDivider(thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(spacing.small))

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
                        Spacer(modifier = Modifier.height(spacing.small))
                        LogAppDangerButton(
                            text = if (isRepairingHealth) "Đang tự động khắc phục..." else "Tự động sửa lỗi dữ liệu (Self-Healing)",
                            onClick = { viewModel.repairDataHealth() },
                            isLoading = isRepairingHealth
                        )
                    }
                }
            }

            // ==========================================
            // 4. GENERAL PREFERENCES (Theme & Day Boundary)
            // ==========================================
            LogAppSectionHeader(
                title = "TÙY CHỌN ỨNG DỤNG",
                subtitle = "Chế độ giao diện và mốc bắt đầu ngày mới"
            )

            LogAppCard {
                Text("Giao diện ứng dụng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Column(Modifier.selectableGroup()) {
                    listOf("system" to "Mặc định hệ thống", "light" to "Giao diện sáng", "dark" to "Giao diện tối").forEach { (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (value == themeMode),
                                    onClick = { viewModel.setThemeMode(value) },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (value == themeMode), onClick = null)
                            Text(text = label, modifier = Modifier.padding(start = spacing.small))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.medium))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(spacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mốc bắt đầu ngày mới", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Các hoạt động đêm khuya trước mốc này vẫn tính vào ngày hôm trước.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = { showTimePickerDialog = true }) {
                        Text(dayBoundary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.extraLarge))
        }
    }

    // Google Drive Backups List Dialog
    if (showDriveBackupsDialog) {
        AlertDialog(
            onDismissRequest = { showDriveBackupsDialog = false },
            title = { Text("Bản sao lưu trên Google Drive") },
            text = {
                if (isLoadingDriveBackups) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (driveBackups.isEmpty()) {
                    Text("Không tìm thấy bản sao lưu nào trên tài khoản Google Drive này.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing.small)
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
                                Column(modifier = Modifier.padding(spacing.medium)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (file.createdTime > 0) {
                                        val formattedDate = java.time.Instant.ofEpochMilli(file.createdTime)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
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
                    Text("Đóng")
                }
            }
        )
    }

    // Enhanced Import Preview Dialog with Strategy Selection
    importPreviewData?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            isImporting = isImporting,
            onDismiss = { viewModel.clearImportPreview() },
            onConfirm = { strategy -> viewModel.confirmImport(strategy) }
        )
    }

    // Day Boundary Time Picker Dialog
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
