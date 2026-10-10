package com.example.logapp.feature.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showSplitTimePicker by remember { mutableStateOf(false) }
    
    val initialSplitLocal = remember(startLocal, endLocal) {
        if (endLocal != null) {
            val midpointMillis = (startLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + 
                                  endLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) / 2
            LocalDateTime.ofInstant(Instant.ofEpochMilli(midpointMillis), ZoneId.systemDefault())
        } else {
            LocalDateTime.now()
        }
    }

    val formatter = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = activity?.name ?: "Unknown Activity",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bắt đầu:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = editedStart.format(formatter),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showStartTimePicker = true }
                                .padding(4.dp)
                        )
                    }

                    if (editedEnd != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kết thúc:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = editedEnd!!.format(formatter),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showEndTimePicker = true }
                                    .padding(4.dp)
                            )
                        }
                    } else {
                        Text("Đang diễn ra...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú (Note)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            val isError = editedEnd != null && editedStart.isAfter(editedEnd)
            if (isError) {
                Text("Lỗi: Giờ kết thúc không thể trước giờ bắt đầu", color = MaterialTheme.colorScheme.error)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showSplitTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Chia", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val finalStart = editedStart.atZone(ZoneId.systemDefault()).toInstant()
                        val finalEnd = editedEnd?.atZone(ZoneId.systemDefault())?.toInstant()
                        onSave(finalStart, finalEnd, note)
                    },
                    modifier = Modifier.weight(1.5f),
                    enabled = !isError
                ) {
                    Text("Lưu", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showSplitTimePicker) {
        TimePickerDialogWrapper(
            initialTime = initialSplitLocal,
            onConfirm = { splitTimeLocal ->
                val finalSplit = splitTimeLocal.atZone(ZoneId.systemDefault()).toInstant()
                onSplit(finalSplit)
                showSplitTimePicker = false
            },
            onDismiss = { showSplitTimePicker = false }
        )
    }

    if (showStartTimePicker) {
        TimePickerDialogWrapper(
            initialTime = editedStart,
            onConfirm = { newTime ->
                editedStart = newTime
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker && editedEnd != null) {
        TimePickerDialogWrapper(
            initialTime = editedEnd!!,
            onConfirm = { newTime ->
                editedEnd = newTime
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
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
        title = { Text("Chọn Giờ") },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(onClick = {
                val newTime = initialTime.withHour(state.hour).withMinute(state.minute)
                onConfirm(newTime)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
