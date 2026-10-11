package com.example.logapp.feature.management.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logapp.core.designsystem.component.*
import com.example.logapp.core.designsystem.tokens.spacing
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.feature.management.model.ActivityPresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFormBottomSheet(
    activity: ActivityEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Long, icon: String?) -> Unit,
    onArchive: (id: String) -> Unit,
    onUnarchive: (id: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onValidateName: (id: String?, name: String) -> String?
) {
    var name by remember { mutableStateOf(activity?.name ?: "") }
    var selectedColor by remember {
        mutableStateOf(activity?.color?.let { Color(it.toInt()) } ?: ActivityPresets.colors.first())
    }
    var selectedIcon by remember { mutableStateOf(activity?.icon) }

    val validationError = remember(name) {
        if (name.isNotBlank()) onValidateName(activity?.id, name) else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.extraLarge)
        ) {
            LogAppBottomSheetHeader(
                title = if (activity == null) "Thêm hoạt động mới" else "Chỉnh sửa hoạt động",
                subtitle = "Tùy chỉnh tên, màu sắc nhận diện và biểu tượng",
                onClose = onDismiss
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Live Preview Card
                LiveActivityPreview(
                    name = name.ifBlank { "Tên hoạt động" },
                    color = selectedColor,
                    icon = selectedIcon
                )

                // Name Input Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên hoạt động *") },
                    placeholder = { Text("Ví dụ: Lập trình, Đọc sách, Thể thao...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = validationError != null,
                    supportingText = {
                        if (validationError != null) {
                            Text(text = validationError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(text = "Tên ngắn gọn, dễ phân biệt khi ghi nhận")
                        }
                    }
                )

                // Color Selection Section
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text(
                        text = "Màu sắc nhận diện",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // 2 rows of 8 colors
                    val firstRowColors = ActivityPresets.colors.take(8)
                    val secondRowColors = ActivityPresets.colors.drop(8)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        firstRowColors.forEach { color ->
                            ColorCircle(
                                color = color,
                                isSelected = color.toArgb() == selectedColor.toArgb(),
                                onClick = { selectedColor = color }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        secondRowColors.forEach { color ->
                            ColorCircle(
                                color = color,
                                isSelected = color.toArgb() == selectedColor.toArgb(),
                                onClick = { selectedColor = color }
                            )
                        }
                    }
                }

                // Icon Selection Section
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Biểu tượng đại diện",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedIcon != null) {
                            TextButton(onClick = { selectedIcon = null }) {
                                Text("Dùng chữ cái đầu", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Horizontal scrolling list of preset icons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        // Default letter chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedIcon == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selectedIcon == null) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { selectedIcon = null }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name.take(1).uppercase().ifBlank { "A" },
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedIcon == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Presets
                        ActivityPresets.iconPresets.forEach { preset ->
                            val isSelected = selectedIcon == preset.icon
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { selectedIcon = preset.icon }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = preset.icon,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.extraSmall))

                // Save Action
                LogAppPrimaryButton(
                    text = if (activity == null) "Tạo hoạt động mới" else "Lưu thay đổi",
                    enabled = name.isNotBlank() && validationError == null,
                    onClick = {
                        if (name.isNotBlank() && validationError == null) {
                            onSave(name.trim(), selectedColor.toArgb().toLong(), selectedIcon)
                        }
                    }
                )

                // Archive / Unarchive Action
                if (activity != null) {
                    if (activity.isArchived) {
                        LogAppSecondaryButton(
                            text = "Khôi phục hoạt động (Unarchive)",
                            onClick = { onUnarchive(activity.id) }
                        )
                    } else {
                        LogAppSecondaryButton(
                            text = "Lưu trữ hoạt động (Archive)",
                            onClick = { onArchive(activity.id) }
                        )
                    }

                    // Delete Action
                    LogAppDangerButton(
                        text = "Xóa hoạt động",
                        icon = Icons.Default.Delete,
                        onClick = { onDelete(activity.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveActivityPreview(
    name: String,
    color: Color,
    icon: String?
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(spacing.medium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                if (!icon.isNullOrBlank()) {
                    Text(text = icon, fontSize = 22.sp)
                } else {
                    Text(
                        text = name.take(1).uppercase().ifBlank { "?" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }

            Spacer(modifier = Modifier.width(spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Bản xem trước hiển thị trên ứng dụng",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
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
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
