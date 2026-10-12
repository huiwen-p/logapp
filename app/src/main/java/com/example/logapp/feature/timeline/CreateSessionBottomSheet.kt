package com.example.logapp.feature.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionBottomSheet(
    activities: List<ActivityEntity>,
    selectedDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSave: (activityId: String, newStart: Instant, newEnd: Instant, newNote: String?) -> Unit
) {
    var note by remember { mutableStateOf("") }

    val initialStart = remember(selectedDate) {
        if (selectedDate == LocalDate.now()) {
            val now = LocalDateTime.now()
            if (now.hour == 0 && now.minute < 30) {
                selectedDate.atTime(0, 0)
            } else {
                now.minusHours(1)
            }
        } else {
            selectedDate.atTime(9, 0)
        }
    }

    val initialEnd = remember(selectedDate) {
        if (selectedDate == LocalDate.now()) {
            LocalDateTime.now()
        } else {
            selectedDate.atTime(10, 0)
        }
    }

    var editedStart by remember { mutableStateOf(initialStart) }
    var editedEnd by remember { mutableStateOf(initialEnd) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }
    var selectedActivity by remember { mutableStateOf<ActivityEntity?>(activities.firstOrNull()) }

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
                title = "Thêm Nhật ký Thủ công",
                subtitle = "Tạo một phiên hoạt động trong quá khứ",
                onClose = { dismissSmoothly() }
            )

            // Activity Selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedActivity?.name ?: "Chọn Hoạt động",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hoạt động") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    activities.forEach { activity ->
                        DropdownMenuItem(
                            text = { Text(activity.name) },
                            onClick = {
                                selectedActivity = activity
                                expanded = false
                            }
                        )
                    }
                }
            }

            LogAppCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Start Time Row
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

                // End Time Row
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
                                text = editedEnd.format(dateFormatter),
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
                                text = editedEnd.format(timeFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Overnight note
            val isOvernight = editedStart.toLocalDate() != editedEnd.toLocalDate()
            if (isOvernight && editedEnd.isAfter(editedStart)) {
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
                placeholder = { Text("Ghi chú thêm về phiên...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            val isError = !editedEnd.isAfter(editedStart)
            if (isError) {
                Text(
                    text = "Lỗi: Giờ kết thúc phải diễn ra sau giờ bắt đầu",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (selectedActivity == null) {
                Text(
                    text = "Lỗi: Vui lòng chọn một hoạt động",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            LogAppPrimaryButton(
                text = "Tạo phiên",
                onClick = {
                    val finalStart = editedStart.atZone(ZoneId.systemDefault()).toInstant()
                    val finalEnd = editedEnd.atZone(ZoneId.systemDefault()).toInstant()
                    selectedActivity?.let { act ->
                        dismissSmoothly {
                            onSave(act.id, finalStart, finalEnd, note.takeIf { n -> n.isNotBlank() })
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isError && selectedActivity != null
            )
        }
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

    if (showEndDatePicker) {
        LogAppDatePickerDialog(
            initialDate = editedEnd.toLocalDate(),
            onConfirm = { newDate ->
                editedEnd = editedEnd.withYear(newDate.year).withMonth(newDate.monthValue).withDayOfMonth(newDate.dayOfMonth)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    if (showEndTimePicker) {
        LogAppTimePickerDialog(
            initialTime = editedEnd.toLocalTime(),
            onConfirm = { newTime ->
                editedEnd = editedEnd.withHour(newTime.hour).withMinute(newTime.minute)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}
