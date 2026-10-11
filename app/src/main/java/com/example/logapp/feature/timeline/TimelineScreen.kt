package com.example.logapp.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logapp.core.designsystem.component.*
import com.example.logapp.core.designsystem.tokens.spacing
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedSession by remember { mutableStateOf<ActivitySessionEntity?>(null) }
    var showCreateSession by remember { mutableStateOf(false) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedSessionIds by remember { mutableStateOf(setOf<String>()) }
    var showMergeDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Scaffold(
        snackbarHost = { LogAppSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (isMultiSelectMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedSessionIds.size}/2 đã chọn để gộp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedSessionIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Hủy chọn")
                        }
                    },
                    actions = {
                        if (selectedSessionIds.size == 2) {
                            Button(
                                onClick = { showMergeDialog = true },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Gộp", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Nhật ký",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSession = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm thủ công")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Multi-select banner hint
            if (isMultiSelectMode) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.medium, vertical = spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (selectedSessionIds.size < 2) {
                                "Chạm phiên thứ 2 liền kề để gộp cùng phiên đã chọn."
                            } else {
                                "Đã chọn 2 phiên. Bấm nút 'Gộp' ở góc trên để xác nhận."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Date Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium, vertical = spacing.extraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changeDate(-1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Ngày trước")
                }
                Text(
                    text = state.selectedDate.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { viewModel.changeDate(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Ngày sau")
                }
            }

            // Day Summary Bar: Total sessions, Total tracked duration, Overlap status
            val trackedDuration = state.dailyAnalytics?.trackedDuration ?: Duration.ZERO
            val overlapDuration = state.dailyAnalytics?.overlapDuration ?: Duration.ZERO
            val hasOverlap = overlapDuration.inWholeMinutes > 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium, vertical = spacing.extraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.sessions.size} phiên",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatDuration(trackedDuration),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.sessions.isNotEmpty()) {
                    if (hasOverlap) {
                        LogAppStatusBadge(
                            status = StatusType.WARNING,
                            text = "Trùng lặp: ${overlapDuration.inWholeMinutes}p",
                            icon = Icons.Default.Warning
                        )
                    } else {
                        LogAppStatusBadge(
                            status = StatusType.SUCCESS,
                            text = "Không trùng lặp"
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = spacing.extraSmall),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Timeline Canvas or Empty State
            Box(modifier = Modifier.weight(1f)) {
                com.example.logapp.feature.timeline.components.TimelineCanvas(
                    selectedDate = state.selectedDate,
                    sessions = state.sessions,
                    activities = state.activities,
                    onSessionClick = { session ->
                        if (isMultiSelectMode) {
                            if (selectedSessionIds.contains(session.id)) {
                                selectedSessionIds = selectedSessionIds - session.id
                                if (selectedSessionIds.isEmpty()) isMultiSelectMode = false
                            } else {
                                if (selectedSessionIds.size < 2) {
                                    selectedSessionIds = selectedSessionIds + session.id
                                }
                            }
                        } else {
                            selectedSession = session
                        }
                    },
                    onSessionLongClick = { session ->
                        isMultiSelectMode = true
                        selectedSessionIds = setOf(session.id)
                    },
                    selectedSessionIds = selectedSessionIds,
                    modifier = Modifier.fillMaxSize()
                )

                if (state.sessions.isEmpty()) {
                    LogAppCard(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = spacing.large)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.small),
                            modifier = Modifier.fillMaxWidth().padding(spacing.small)
                        ) {
                            Text(
                                text = "Chưa có phiên nhật ký nào",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ngày này chưa có hoạt động nào được ghi nhận. Bạn có thể thêm thủ công hoặc bắt đầu ghi nhận từ màn hình Theo dõi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = spacing.small)
                            )
                            Spacer(modifier = Modifier.height(spacing.extraSmall))
                            LogAppPrimaryButton(
                                text = "Thêm phiên thủ công",
                                onClick = { showCreateSession = true }
                            )
                        }
                    }
                }
            }
        }

        // Session Edit Bottom Sheet
        selectedSession?.let { session ->
            val activity = state.activities.find { it.id == session.activityId }
            SessionBottomSheet(
                session = session,
                activity = activity,
                onDismiss = { selectedSession = null },
                onSave = { newStart, newEnd, newNote ->
                    viewModel.editSession(session, newStart, newEnd, newNote)
                    selectedSession = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Đã cập nhật phiên thành công")
                    }
                },
                onDelete = {
                    viewModel.deleteSession(session)
                    selectedSession = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Đã xóa phiên ghi nhận")
                    }
                },
                onSplit = { splitTime ->
                    viewModel.splitSession(session, splitTime)
                    selectedSession = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Đã chia phiên thành công")
                    }
                }
            )
        }

        // Manual Session Creation Bottom Sheet
        if (showCreateSession) {
            CreateSessionBottomSheet(
                activities = state.activities,
                onDismiss = { showCreateSession = false },
                onSave = { activityId, start, end, note ->
                    viewModel.createManualSession(activityId, start, end, note)
                    showCreateSession = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Đã thêm phiên mới thành công")
                    }
                }
            )
        }

        // Merge Sessions Confirmation Dialog
        if (showMergeDialog && selectedSessionIds.size == 2) {
            val s1 = state.sessions.find { it.id == selectedSessionIds.elementAtOrNull(0) }
            val s2 = state.sessions.find { it.id == selectedSessionIds.elementAtOrNull(1) }

            if (s1 != null && s2 != null) {
                val (first, second) = if (s1.startedAt.isBefore(s2.startedAt)) s1 to s2 else s2 to s1
                val sameActivity = s1.activityId == s2.activityId
                val noGap = first.endedAt == null || !first.endedAt.isBefore(second.startedAt)
                val canMerge = sameActivity && noGap

                val activity = state.activities.find { it.id == s1.activityId }
                val activityName = activity?.name ?: "Hoạt động"

                if (canMerge) {
                    val mergedStartStr = first.startedAt.atZone(ZoneId.systemDefault()).format(timeFormatter)
                    val secondEnd = second.endedAt ?: Instant.now()
                    val mergedEnd = if (first.endedAt == null || second.endedAt == null) null else {
                        if (first.endedAt.isAfter(secondEnd)) first.endedAt else secondEnd
                    }
                    val mergedEndStr = mergedEnd?.atZone(ZoneId.systemDefault())?.format(timeFormatter) ?: "Hiện tại"

                    LogAppConfirmDialog(
                        title = "Gộp 2 phiên hoạt động?",
                        message = "Gộp 2 phiên của \"$activityName\":\n\n" +
                                "• Khoảng thời gian mới: $mergedStartStr → $mergedEndStr\n" +
                                "• Ghi chú của cả 2 phiên sẽ được tự động liên kết.\n\n" +
                                "Bạn có muốn tiếp tục không?",
                        confirmText = "Gộp phiên",
                        dismissText = "Hủy",
                        onConfirm = {
                            viewModel.mergeSessions(s1, s2)
                            isMultiSelectMode = false
                            selectedSessionIds = emptySet()
                            showMergeDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Đã gộp 2 phiên thành công")
                            }
                        },
                        onDismiss = { showMergeDialog = false }
                    )
                } else {
                    val errorMessage = if (!sameActivity) {
                        val act2 = state.activities.find { it.id == s2.activityId }
                        "Hai phiên được chọn thuộc hai hoạt động khác nhau (\"${activity?.name}\" và \"${act2?.name}\"). Chỉ có thể gộp các phiên cùng hoạt động."
                    } else {
                        "Hai phiên có khoảng cách thời gian ngắt quãng ở giữa. Chỉ có thể gộp các phiên liền kề hoặc trùng lặp thời gian."
                    }

                    LogAppConfirmDialog(
                        title = "Không thể gộp 2 phiên",
                        message = errorMessage,
                        confirmText = "Đã hiểu",
                        dismissText = "Đóng",
                        onConfirm = { showMergeDialog = false },
                        onDismiss = { showMergeDialog = false }
                    )
                }
            }
        }
    }
}

private fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}p" else "${minutes}p"
}
