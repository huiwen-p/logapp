package com.example.logapp.feature.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logapp.core.designsystem.component.*
import com.example.logapp.core.designsystem.tokens.spacing
import com.example.logapp.domain.model.backup.BackupSchemaV1
import com.example.logapp.domain.usecase.ImportStrategy

@Composable
fun ImportPreviewDialog(
    preview: BackupSchemaV1,
    isImporting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (strategy: ImportStrategy) -> Unit
) {
    var selectedStrategy by remember { mutableStateOf(ImportStrategy.MERGE) }
    var showReplaceConfirmation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = {
            Text(
                text = "Xem trước tệp sao lưu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Text(
                    text = "Tệp sao lưu hợp lệ (Schema V1). Vui lòng chọn cách thức khôi phục dữ liệu vào ứng dụng:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Stats Summary Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(spacing.medium)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${preview.activities.size}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Hoạt động",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(36.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${preview.sessions.size}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Phiên ghi nhận",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Strategy Selection Options
                Text(
                    text = "Chiến lược nhập dữ liệu:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // 1. Merge Strategy
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedStrategy == ImportStrategy.MERGE) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (selectedStrategy == ImportStrategy.MERGE) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedStrategy = ImportStrategy.MERGE }
                ) {
                    Row(
                        modifier = Modifier.padding(spacing.medium),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = selectedStrategy == ImportStrategy.MERGE,
                            onClick = { selectedStrategy = ImportStrategy.MERGE }
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Gộp dữ liệu (Merge)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.width(spacing.small))
                                LogAppStatusBadge(
                                    status = StatusType.SUCCESS,
                                    text = "Khuyên dùng"
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Giữ nguyên dữ liệu hiện có trên thiết bị, bổ sung thêm các hoạt động và phiên mới từ file sao lưu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Replace Strategy
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedStrategy == ImportStrategy.REPLACE) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (selectedStrategy == ImportStrategy.REPLACE) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedStrategy = ImportStrategy.REPLACE }
                ) {
                    Row(
                        modifier = Modifier.padding(spacing.medium),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = selectedStrategy == ImportStrategy.REPLACE,
                            onClick = { selectedStrategy = ImportStrategy.REPLACE },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ghi đè toàn bộ (Replace)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selectedStrategy == ImportStrategy.REPLACE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(spacing.small))
                                LogAppStatusBadge(
                                    status = StatusType.WARNING,
                                    text = "Nguy hiểm"
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Xóa toàn bộ các hoạt động và phiên hiện tại trên máy, thay thế hoàn toàn bằng nội dung file sao lưu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedStrategy == ImportStrategy.REPLACE) {
                        showReplaceConfirmation = true
                    } else {
                        onConfirm(ImportStrategy.MERGE)
                    }
                },
                enabled = !isImporting
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text("Đang nhập...")
                } else {
                    Text(if (selectedStrategy == ImportStrategy.MERGE) "Xác nhận Gộp dữ liệu" else "Tiếp tục Ghi đè")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting
            ) {
                Text("Hủy")
            }
        }
    )

    // Secondary Safety Confirmation Dialog for Dangerous Replace
    if (showReplaceConfirmation) {
        LogAppConfirmDialog(
            title = "Cảnh báo ghi đè dữ liệu",
            message = "Hành động này sẽ XÓA TOÀN BỘ hoạt động và phiên ghi nhận hiện có trên thiết bị của bạn để thay thế bằng dữ liệu từ file sao lưu. Hành động này không thể hoàn tác!\n\nBạn có chắc chắn muốn tiếp tục?",
            confirmText = "Ghi đè tất cả",
            dismissText = "Quay lại",
            isDestructive = true,
            icon = Icons.Default.Warning,
            onConfirm = {
                showReplaceConfirmation = false
                onConfirm(ImportStrategy.REPLACE)
            },
            onDismiss = { showReplaceConfirmation = false }
        )
    }
}
