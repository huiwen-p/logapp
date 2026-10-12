package com.example.logapp.feature.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logapp.core.designsystem.component.*
import com.example.logapp.core.designsystem.tokens.spacing
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionBottomSheet(
    session: ActivitySessionEntity,
    activity: ActivityEntity?,
    onDismiss: () -> Unit,
    onSave: (newStart: Instant, newEnd: Instant?, newNote: String?) -> Unit,
    onDelete: () -> Unit,
    onSplit: (splitTime: Instant) -> Unit
) {
    var note by remember { mutableStateOf(session.note ?: "") }
    
    // Time states in local zone
    val startLocal = remember(session.startedAt) { 
        LocalDateTime.ofInstant(session.startedAt, ZoneId.systemDefault()) 
    }
    val endLocal = remember(session.endedAt) { 
        session.endedAt?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) } 
    }

    var editedStart by remember { mutableStateOf(startLocal) }
    var editedEnd by remember { mutableStateOf(endLocal) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    val initialSplitLocal = remember(startLocal, endLocal) {
        if (endLocal != null) {
            val midpointMillis = (startLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + 
                                  endLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) / 2
            LocalDateTime.ofInstant(Instant.ofEpochMilli(midpointMillis), ZoneId.systemDefault())
        } else {
            LocalDateTime.now()
        }
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }

    fun dismissSmoothly(action: () -> Unit = onDismiss) {
        if (isDismissing) return
        isDismissing = true
        coroutineScope.launch {
            sheetState.hide()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { dismissSmoothly() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium)
                .padding(bottom = spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            LogAppBottomSheetHeader(
                title = activity?.name ?: "Phiên hoạt động",
                subtitle = "Chi tiết và chỉnh sửa phiên ghi nhận",
                onClose = { dismissSmoothly() }
            )

            LogAppCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Bắt đầu:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showStartDatePicker = true }
                        ) {
                            Text(
                                text = editedStart.format(dateFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showStartTimePicker = true }
                        ) {
                            Text(
                                text = editedStart.format(timeFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.small))

                if (editedEnd != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Kết thúc:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showEndDatePicker = true }
                            ) {
                                Text(
                                    text = editedEnd!!.format(dateFormatter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showEndTimePicker = true }
                            ) {
                                Text(
                                    text = editedEnd!!.format(timeFormatter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Trạng thái:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LogAppStatusBadge(
                            status = StatusType.ACTIVE,
                            text = "Đang diễn ra"
                        )
                    }
                }
            }

            // Overnight note
            val isOvernight = editedEnd != null && editedStart.toLocalDate() != editedEnd!!.toLocalDate()
            if (isOvernight && editedEnd != null && editedEnd!!.isAfter(editedStart)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Phiên kéo dài qua nửa đêm. Thời lượng sẽ được tự động phân bổ chính xác theo từng ngày trên Nhật ký và Thống kê.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }


            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú (Note)") },
                placeholder = { Text("Thêm ghi chú cho phiên này...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            val isError = editedEnd != null && editedStart.isAfter(editedEnd)
            if (isError) {
                Text(
                    text = "Lỗi: Giờ kết thúc không thể diễn ra trước giờ bắt đầu",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                LogAppDangerButton(
                    text = "Xóa",
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.weight(1f)
                )

                LogAppSecondaryButton(
                    text = "Chia",
                    onClick = { showSplitDialog = true },
                    modifier = Modifier.weight(1f)
                )

                LogAppPrimaryButton(
                    text = "Lưu",
                    onClick = {
                        val finalStart = editedStart.atZone(ZoneId.systemDefault()).toInstant()
                        val finalEnd = editedEnd?.atZone(ZoneId.systemDefault())?.toInstant()
                        dismissSmoothly { onSave(finalStart, finalEnd, note) }
                    },
                    enabled = !isError,
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        LogAppConfirmDialog(
            title = "Xóa phiên hoạt động?",
            message = "Bạn có chắc chắn muốn xóa phiên ghi nhận này không? Thao tác này không thể hoàn tác.",
            onConfirm = {
                showDeleteConfirmDialog = false
                dismissSmoothly { onDelete() }
            },
            onDismiss = { showDeleteConfirmDialog = false },
            confirmText = "Xóa",
            isDestructive = true
        )
    }

    if (showSplitDialog) {
        SplitSessionDialog(
            session = session,
            initialSplitTime = initialSplitLocal,
            onConfirm = { splitInstant ->
                showSplitDialog = false
                dismissSmoothly { onSplit(splitInstant) }
            },
            onDismiss = { showSplitDialog = false }
        )
    }

    if (showStartDatePicker) {
        LogAppDatePickerDialog(
            initialDate = editedStart.toLocalDate(),
            onConfirm = { newDate ->
                editedStart = editedStart.withYear(newDate.year).withMonth(newDate.monthValue).withDayOfMonth(newDate.dayOfMonth)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showStartTimePicker) {
        LogAppTimePickerDialog(
            initialTime = editedStart.toLocalTime(),
            onConfirm = { newTime ->
                editedStart = editedStart.withHour(newTime.hour).withMinute(newTime.minute)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndDatePicker && editedEnd != null) {
        LogAppDatePickerDialog(
            initialDate = editedEnd!!.toLocalDate(),
            onConfirm = { newDate ->
                editedEnd = editedEnd!!.withYear(newDate.year).withMonth(newDate.monthValue).withDayOfMonth(newDate.dayOfMonth)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    if (showEndTimePicker && editedEnd != null) {
        LogAppTimePickerDialog(
            initialTime = editedEnd!!.toLocalTime(),
            onConfirm = { newTime ->
                editedEnd = editedEnd!!.withHour(newTime.hour).withMinute(newTime.minute)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitSessionDialog(
    session: ActivitySessionEntity,
    initialSplitTime: LocalDateTime,
    onConfirm: (Instant) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialSplitTime.hour,
        initialMinute = initialSplitTime.minute,
        is24Hour = true
    )

    val currentSplitTime = initialSplitTime.withHour(state.hour).withMinute(state.minute)
    val currentSplitInstant = currentSplitTime.atZone(ZoneId.systemDefault()).toInstant()

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val fullFormatter = DateTimeFormatter.ofPattern("HH:mm, dd/MM")

    val startInstant = session.startedAt
    val endInstant = session.endedAt ?: Instant.now()

    val isAfterStart = currentSplitInstant.isAfter(startInstant)
    val isBeforeEnd = currentSplitInstant.isBefore(endInstant)
    val isValid = isAfterStart && isBeforeEnd

    val startStr = startInstant.atZone(ZoneId.systemDefault()).format(fullFormatter)
    val endStr = if (session.endedAt != null) {
        session.endedAt.atZone(ZoneId.systemDefault()).format(fullFormatter)
    } else {
        "Hiện tại"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chia phiên hoạt động",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Khoảng thời gian: $startStr → $endStr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TimePicker(state = state)

                if (!isValid) {
                    Text(
                        text = "⚠️ Thời gian chia (${currentSplitTime.format(timeFormatter)}) phải nằm giữa thời điểm bắt đầu và kết thúc.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    val splitStr = currentSplitTime.format(timeFormatter)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Xem trước sau khi chia:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• Phiên 1: $startStr → $splitStr\n• Phiên 2: $splitStr → $endStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentSplitInstant) },
                enabled = isValid
            ) {
                Text(
                    text = "Xác nhận chia",
                    fontWeight = FontWeight.Bold,
                    color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogWrapper(
    initialTime: LocalDateTime,
    onConfirm: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn Giờ", fontWeight = FontWeight.Bold) },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(onClick = {
                val newTime = initialTime.withHour(state.hour).withMinute(state.minute)
                onConfirm(newTime)
            }) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
