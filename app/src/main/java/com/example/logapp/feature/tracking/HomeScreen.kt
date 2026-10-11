package com.example.logapp.feature.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logapp.core.designsystem.component.LogAppElevatedCard
import com.example.logapp.core.designsystem.component.LogAppEmptyState
import com.example.logapp.core.designsystem.component.LogAppErrorState
import com.example.logapp.core.designsystem.component.LogAppOfflineBanner
import com.example.logapp.core.designsystem.component.LogAppPrimaryButton
import com.example.logapp.core.designsystem.component.LogAppSecondaryButton
import com.example.logapp.core.designsystem.component.LogAppSectionHeader
import com.example.logapp.core.designsystem.component.LogAppSnackbarHost
import com.example.logapp.core.designsystem.component.LogAppStatusBadge
import com.example.logapp.core.designsystem.component.SkeletonActivityGrid
import com.example.logapp.core.designsystem.component.StatusType
import com.example.logapp.core.designsystem.tokens.spacing
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.feature.components.ActivityCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToActivities: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val activities by viewModel.unarchivedActivities.collectAsState()
    val filteredActivities by viewModel.filteredActivities.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val todaySessionsCount by viewModel.todaySessionsCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { LogAppSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TimeLens",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.displayMedium
                    )
                },
                actions = {
                    TextButton(onClick = onNavigateToActivities) {
                        Text("Quản lý", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.medium)
        ) {
            // 0. Offline Banner (reassures offline-first storage)
            LogAppOfflineBanner(isOffline = !isOnline)

            // 0.1 Error State with Retry
            if (errorMessage != null) {
                LogAppErrorState(
                    title = "Không thể tải dữ liệu hoạt động",
                    message = errorMessage ?: "Đã xảy ra lỗi không xác định.",
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.padding(bottom = spacing.medium)
                )
            }

            // 1. Active Sessions Section
            AnimatedVisibility(
                visible = activeSessions.isNotEmpty(),
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column(modifier = Modifier.padding(bottom = spacing.medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ĐANG THEO DÕI",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LogAppStatusBadge(
                            status = StatusType.ACTIVE,
                            text = "${activeSessions.size} đang chạy"
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                        activeSessions.forEach { session ->
                            val activity = activities.find { it.id == session.activityId }
                            val activityName = activity?.name ?: "Chưa xác định"
                            ActiveSessionItem(
                                session = session,
                                activityName = activityName,
                                activityColor = activity?.color,
                                icon = activity?.icon,
                                onStop = {
                                    viewModel.stopSession(session)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Đã dừng $activityName",
                                            actionLabel = "Hoàn tác",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoStopSession(session)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 1.1 Motivating hint when user hasn't tracked anything today
            if (activities.isNotEmpty() && activeSessions.isEmpty() && todaySessionsCount == 0 && !isLoading) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.small)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Bạn chưa ghi nhận hoạt động nào hôm nay. Chạm một hoạt động bên dưới để bắt đầu!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Quick Tracking Section Header
            LogAppSectionHeader(
                title = "QUICK TRACKING",
                subtitle = "Chạm để bắt đầu ghi nhận ngay lập tức"
            )

            // Quick Search Bar (when there are 6+ activities or user is searching)
            if (activities.size >= 6 || searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Tìm nhanh hoạt động...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Xóa")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.small)
                )
            }

            // 3. Loading / Grid / Empty States
            if (isLoading && activities.isEmpty()) {
                // Loading Skeleton State
                SkeletonActivityGrid(
                    itemCount = 6,
                    modifier = Modifier.weight(1f)
                )
            } else if (activities.isEmpty()) {
                // Edge Case 1: Empty State - No activities created yet
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LogAppEmptyState(
                        icon = Icons.Default.PlayArrow,
                        title = "Bắt đầu ghi nhận thời gian",
                        message = "Tạo các hoạt động quen thuộc như Học tập, Làm việc, Đọc sách để bắt đầu quan sát bức tranh thực tế của bạn.",
                        primaryButtonText = "Tạo hoạt động mới",
                        primaryButtonIcon = Icons.Default.Add,
                        onPrimaryClick = onNavigateToActivities,
                        secondaryButtonText = "Thêm hoạt động mẫu",
                        onSecondaryClick = { viewModel.addSampleActivities() }
                    )
                }
            } else if (filteredActivities.isEmpty() && searchQuery.isNotBlank()) {
                // Edge Case 2: Empty Search Result
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Text(
                            text = "Không tìm thấy hoạt động \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Text("Xem tất cả hoạt động", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Regular Activity Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredActivities, key = { it.id }) { activity ->
                        val isRunning = activeSessions.any { it.activityId == activity.id }
                        ActivityCard(
                            name = activity.name,
                            color = activity.color,
                            icon = activity.icon,
                            isActive = isRunning,
                            onClick = { viewModel.startActivity(activity.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveSessionItem(
    session: ActivitySessionEntity,
    activityName: String,
    activityColor: Long? = null,
    icon: String? = null,
    onStop: () -> Unit
) {
    var duration by remember { mutableStateOf(Duration.between(session.startedAt, Instant.now())) }

    LaunchedEffect(session.startedAt) {
        while (true) {
            duration = Duration.between(session.startedAt, Instant.now())
            delay(1000)
        }
    }

    val hours = duration.toHours()
    val minutes = duration.toMinutesPart()
    val seconds = duration.toSecondsPart()
    val durationString = "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    val iconColor = activityColor?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulse indicator dot with Activity color
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
            )

            Spacer(modifier = Modifier.width(spacing.small))

            // Initial icon letter or emoji
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!icon.isNullOrBlank()) {
                    Text(
                        text = icon,
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        text = activityName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = iconColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(spacing.small))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activityName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
                Text(
                    text = durationString,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }

            FilledTonalButton(
                onClick = onStop,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Dừng", fontWeight = FontWeight.Bold)
            }
        }
    }
}
