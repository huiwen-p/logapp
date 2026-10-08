package com.example.logapp.feature.management.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.logapp.data.local.entity.ActivityEntity

// Predefined palette for simplicity
private val ColorPalette = listOf(
    Color(0xFF2196F3), // Blue
    Color(0xFF4CAF50), // Green
    Color(0xFFFF9800), // Orange
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFFC107), // Amber
    Color(0xFF607D8B), // Blue Grey
    Color(0xFFF44336), // Red
    Color(0xFF795548), // Brown
    Color(0xFF8BC34A), // Light Green
    Color(0xFF3F51B5)  // Indigo
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFormBottomSheet(
    activity: ActivityEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Long, icon: String) -> Unit,
    onArchive: (id: String) -> Unit,
    onUnarchive: (id: String) -> Unit
) {
    var name by remember { mutableStateOf(activity?.name ?: "") }
    var selectedColor by remember { 
        mutableStateOf(activity?.color?.let { Color(it) } ?: ColorPalette.first()) 
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = if (activity == null) "Thêm Hoạt động mới" else "Chỉnh sửa Hoạt động",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên hoạt động") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Màu sắc", style = MaterialTheme.typography.titleMedium)
                
                // Color grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ColorPalette.take(6).forEach { color ->
                        ColorCircle(
                            color = color,
                            isSelected = color.toArgb() == selectedColor.toArgb(),
                            onClick = { selectedColor = color }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ColorPalette.drop(6).take(6).forEach { color ->
                        ColorCircle(
                            color = color,
                            isSelected = color.toArgb() == selectedColor.toArgb(),
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedColor.value.toLong(), "default")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Lưu Hoạt động")
            }

            if (activity != null) {
                OutlinedButton(
                    onClick = {
                        if (activity.isArchived) {
                            onUnarchive(activity.id)
                        } else {
                            onArchive(activity.id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (activity.isArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (activity.isArchived) "Khôi phục (Unarchive)" else "Lưu trữ (Archive)")
                }
            }
        }
    }
}

@Composable
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}
