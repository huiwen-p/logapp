package com.example.logapp.feature.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
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
    val isClearingData by viewModel.isClearingData.collectAsState()

    var showDriveBackupsDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
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
            // 1. GIAO DIỆN & TÙY CHỌN (APPEARANCE)
            // ==========================================
            LogAppSectionHeader(
                title = "TÙY CHỌN GIAO DIỆN",
                subtitle = "Chế độ hiển thị sáng / tối cho toàn bộ ứng dụng"
            )

            LogAppCard {
                Text(
                    text = "Giao diện hiển thị",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(spacing.small))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    val themeOptions = listOf(
                        Triple("system", "Hệ thống", "Theo thiết bị"),
                        Triple("light", "Sáng", "Nền sáng"),
                        Triple("dark", "Tối", "Nền tối")
                    )
                    themeOptions.forEach { (mode, title, desc) ->
                        val isSelected = (mode == themeMode)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setThemeMode(mode) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                     else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = spacing.small, horizontal = spacing.extraSmall),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(spacing.extraSmall))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. ĐỒNG BỘ & SAO LƯU ĐÁM MÂY (CLOUD SYNC)
            // ==========================================
            LogAppSectionHeader(
                title = "ĐỒNG BỘ & SAO LƯU ĐÁM MÂY",
                subtitle = "Bảo vệ an toàn và đồng bộ dữ liệu đa thiết bị"
            )

            // Google Drive Backup Card
            LogAppElevatedCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = if (isDriveSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Column {
                            Text(
                                text = "Google Drive",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDriveSignedIn) "Đồng bộ đám mây cá nhân" else "Chưa liên kết tài khoản",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(spacing.small))
                    LogAppStatusBadge(
                        status = if (isDriveSignedIn) StatusType.SUCCESS else StatusType.NEUTRAL,
                        text = if (isDriveSignedIn) "Đã kết nối" else "Chưa kết nối"
                    )
                }

                Spacer(modifier = Modifier.height(spacing.small))

                if (isDriveSignedIn) {
                    signedInEmail?.let { email ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = spacing.small, vertical = spacing.extraSmall),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(spacing.extraSmall))
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(spacing.small))
                    }

                    // Last Backup Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bản sao lưu gần nhất:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lastBackupTimeFormatted ?: "Chưa có bản sao lưu",
                            style = MaterialTheme.typography.bodySmall,
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

                        if (isError) {
                            LogAppErrorState(
                                title = "Sao lưu Google Drive thất bại",
                                message = msg,
                                retryButtonText = "Thử lại",
                                onRetry = { viewModel.triggerBackupNow(context) }
                            )
                        } else {
                            Surface(
                                color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(spacing.small)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.medium))
                    HorizontalDivider(thickness = 0.5.dp)
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
                                text = "Chạy ngầm khi có Wi-Fi và pin không yếu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(spacing.small))
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
                            listOf("daily" to "Hàng ngày", "weekly" to "Hàng tuần", "manual" to "Thủ công").forEach { (freq, label) ->
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
                        text = "Liên kết tài khoản Google Drive để tự động sao lưu dữ liệu cá nhân định kỳ lên đám mây và dễ dàng khôi phục khi đổi thiết bị.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))
                    LogAppPrimaryButton(
                        text = "Liên kết Google Drive",
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

            // Local Device Storage Card (Fix squished badge bug & Friendly copy)
            LogAppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Column {
                            Text(
                                text = "Lưu trữ nội bộ thiết bị",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Offline-First",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(spacing.small))
                    LogAppStatusBadge(
                        status = StatusType.SUCCESS,
                        text = "Ngoại tuyến an toàn"
                    )
                }

                Spacer(modifier = Modifier.height(spacing.small))

                Text(
                    text = "Dữ liệu được lưu trữ an toàn 100% trong bộ nhớ máy. Toàn bộ tính năng hoạt động trọn vẹn ngay cả khi không có kết nối mạng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (pendingSyncCount > 0) {
                    Spacer(modifier = Modifier.height(spacing.small))
                    HorizontalDivider(thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(spacing.small))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bản ghi chờ đồng bộ:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$pendingSyncCount bản ghi",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ==========================================
            // 3. SAO LƯU & XUẤT TỆP TIN CỤC BỘ
            // ==========================================
            LogAppSectionHeader(
                title = "SAO LƯU & XUẤT TỆP TIN",
                subtitle = "Xuất và nhập tệp tin lưu trực tiếp trên thiết bị (100% Offline)"
            )

            LogAppCard {
                Text(
                    text = "Tệp sao lưu độc lập (JSON / CSV) giúp bạn làm chủ trọn vẹn dữ liệu cá nhân, dễ dàng lưu trữ riêng hoặc chia sẻ sang ứng dụng khác.",
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
            // 4. TÌNH TRẠNG CƠ SỞ DỮ LIỆU (DATA HEALTH)
            // ==========================================
            LogAppSectionHeader(
                title = "TÌNH TRẠNG DỮ LIỆU",
                subtitle = "Kiểm tra tính toàn vẹn và độ tin cậy của dữ liệu",
                action = {
                    if (isCheckingHealth) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = { viewModel.checkDataHealth() }) {
                            Text("Quét lại", fontWeight = FontWeight.SemiBold)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tính toàn vẹn dữ liệu",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (report.isHealthy) "Không có lỗi sai lệch hay bản ghi hỏng" else "Phát hiện một số dữ liệu cần khắc phục",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(spacing.small))
                        LogAppStatusBadge(
                            status = if (report.isHealthy) StatusType.SUCCESS else StatusType.WARNING,
                            text = if (report.isHealthy) "Lành mạnh" else "Cần kiểm tra"
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.medium))

                    // Metric summary boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(spacing.small),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${report.totalActivities}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Hoạt động hợp lệ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(spacing.small),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${report.totalSessions}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Phiên ghi nhận",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (report.hasIssues) {
                        Spacer(modifier = Modifier.height(spacing.small))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(spacing.small)) {
                                if (report.orphanSessionsCount > 0) {
                                    Text(
                                        text = "• ${report.orphanSessionsCount} phiên ghi mất liên kết với hoạt động",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                if (report.invalidTimestampCount > 0) {
                                    Text(
                                        text = "• ${report.invalidTimestampCount} phiên có mốc thời gian không nhất quán",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

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
            // 5. VÙNG NGUY HIỂM (DANGER ZONE)
            // ==========================================
            LogAppSectionHeader(
                title = "VÙNG NGUY HIỂM",
                subtitle = "Thao tác không thể hoàn tác trên thiết bị này"
            )

            LogAppCard {
                Text(
                    text = "Xóa toàn bộ dữ liệu ứng dụng",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = "Xóa sạch toàn bộ hoạt động, lịch sử các phiên bấm giờ và khôi phục ứng dụng về trạng thái ban đầu như mới cài đặt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.medium))
                LogAppDangerButton(
                    text = "Xóa toàn bộ dữ liệu...",
                    icon = Icons.Default.Delete,
                    onClick = { showClearDataDialog = true }
                )
            }

            // ==========================================
            // 6. THÔNG TIN ỨNG DỤNG (ABOUT FOOTER)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TimeLens",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = "Phiên bản 1.0.0 • 100% Quyền riêng tư & Lưu trữ cục bộ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        repeat(3) {
                            SkeletonBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                } else if (driveBackups.isEmpty()) {
                    LogAppEmptyState(
                        icon = Icons.Default.Info,
                        title = "Chưa có bản sao lưu",
                        message = "Không tìm thấy bản sao lưu nào của TimeLens trên tài khoản Google Drive này. Hãy bấm 'Sao lưu ngay' để tải bản sao lưu đầu tiên lên."
                    )
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

    // Clear All Data Confirmation Dialog (with 10s countdown & mandatory checkbox)
    if (showClearDataDialog) {
        var countdownSeconds by remember { mutableIntStateOf(10) }
        var isAgreed by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            while (countdownSeconds > 0) {
                delay(1000L)
                countdownSeconds--
            }
        }

        AlertDialog(
            onDismissRequest = {
                if (!isClearingData) showClearDataDialog = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "XÓA TOÀN BỘ DỮ LIỆU",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Text(
                        text = "Cảnh báo: Hành động này có mức độ rủi ro cực cao và KHÔNG THỂ HOÀN TÁC sau khi thực hiện!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(spacing.small),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Dữ liệu sẽ bị xóa sạch bao gồm:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("• Tất cả các hoạt động & danh mục đã tạo", style = MaterialTheme.typography.bodySmall)
                            Text("• Toàn bộ lịch sử các phiên bấm giờ", style = MaterialTheme.typography.bodySmall)
                            Text("• Mọi dữ liệu phân tích, thống kê & ghi chú", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Text(
                        text = "Lưu ý: Thao tác này chỉ xóa dữ liệu trên thiết bị này. Các bản sao lưu trên Google Drive hoặc tệp JSON/CSV đã xuất trước đó sẽ không bị ảnh hưởng.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(spacing.extraSmall))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isClearingData) { isAgreed = !isAgreed }
                            .padding(vertical = spacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAgreed,
                            onCheckedChange = { isAgreed = it },
                            enabled = !isClearingData
                        )
                        Spacer(modifier = Modifier.width(spacing.extraSmall))
                        Text(
                            text = "Tôi hiểu và đồng ý xóa vĩnh viễn toàn bộ dữ liệu trên thiết bị này.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                val canConfirm = (countdownSeconds == 0) && isAgreed && !isClearingData
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    },
                    enabled = canConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                        disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isClearingData) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else if (countdownSeconds > 0) {
                        Text("Xác nhận xóa (${countdownSeconds}s)", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Xác nhận xóa vĩnh viễn", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearDataDialog = false },
                    enabled = !isClearingData,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hủy bỏ")
                }
            }
        )
    }
}
