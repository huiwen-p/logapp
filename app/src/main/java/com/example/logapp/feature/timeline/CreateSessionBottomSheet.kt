package com.example.logapp.feature.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logapp.data.local.entity.ActivityEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionBottomSheet(
    activities: List<ActivityEntity>,
    onDismiss: () -> Unit,
    onSave: (activityId: String, newStart: Instant, newEnd: Instant, newNote: String?) -> Unit
) {
    var note by remember { mutableStateOf("") }
    
    val nowLocal = remember { LocalDateTime.now() }
    var editedStart by remember { mutableStateOf(nowLocal.minusHours(1)) }
    var editedEnd by remember { mutableStateOf(nowLocal) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    var expanded by remember { mutableStateOf(false) }
    var selectedActivity by remember { mutableStateOf<ActivityEntity?>(activities.firstOrNull()) }

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
                text = "Thêm Nhật ký Thủ công",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Activity Selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedActivity?.name ?: "Chọn Activity",
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kết thúc:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = editedEnd.format(formatter),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showEndTimePicker = true }
                                .padding(4.dp)
                        )
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

            val isError = editedStart.isAfter(editedEnd)
            if (isError) {
                Text("Lỗi: Giờ kết thúc không thể trước giờ bắt đầu", color = MaterialTheme.colorScheme.error)
            }
            if (selectedActivity == null) {
                Text("Lỗi: Vui lòng chọn một hoạt động", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val finalStart = editedStart.atZone(ZoneId.systemDefault()).toInstant()
                    val finalEnd = editedEnd.atZone(ZoneId.systemDefault()).toInstant()
                    selectedActivity?.let {
                        onSave(it.id, finalStart, finalEnd, note.takeIf { n -> n.isNotBlank() })
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isError && selectedActivity != null
            ) {
                Text("Thêm", fontWeight = FontWeight.Bold)
            }
        }
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

    if (showEndTimePicker) {
        TimePickerDialogWrapper(
            initialTime = editedEnd,
            onConfirm = { newTime ->
                editedEnd = newTime
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}
