package com.example.logapp.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logapp.core.designsystem.component.*
import com.example.logapp.core.designsystem.tokens.semanticColors
import com.example.logapp.core.designsystem.tokens.spacing
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.domain.engine.DailyAnalytics
import com.example.logapp.domain.engine.MonthlyAnalytics
import com.example.logapp.domain.engine.WeeklyAnalytics
import com.example.logapp.feature.analytics.components.MonthlyHeatmap
import com.example.logapp.feature.analytics.components.RelationshipStats
import com.example.logapp.feature.analytics.components.RingChart
import com.example.logapp.feature.analytics.components.WeeklyBarChart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Phân tích & Thống kê",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Selection (Day / Week / Month)
            SecondaryTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = state.selectedTab == AnalyticsTab.DAILY,
                    onClick = { viewModel.setTab(AnalyticsTab.DAILY) },
                    text = { Text("Ngày (Day)", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = state.selectedTab == AnalyticsTab.WEEKLY,
                    onClick = { viewModel.setTab(AnalyticsTab.WEEKLY) },
                    text = { Text("Tuần (Week)", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = state.selectedTab == AnalyticsTab.MONTHLY,
                    onClick = { viewModel.setTab(AnalyticsTab.MONTHLY) },
                    text = { Text("Tháng (Month)", fontWeight = FontWeight.Bold) }
                )
            }

            // Period Navigation Bar
            PeriodNavigationBar(
                selectedTab = state.selectedTab,
                selectedDate = state.selectedDate,
                onPrevious = { viewModel.previousPeriod() },
                onNext = { viewModel.nextPeriod() },
                onResetToday = { viewModel.resetToCurrentPeriod() }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Scrollable Analytics Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.medium, vertical = spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.large)
            ) {
                if (state.errorMessage != null) {
                    LogAppErrorState(
                        title = "Không thể tính toán thống kê",
                        message = state.errorMessage ?: "Đã xảy ra lỗi không xác định.",
                        onRetry = { viewModel.retry() }
                    )
                } else if (state.isLoading) {
                    SkeletonAnalytics()
                } else {
                    when (state.selectedTab) {
                        AnalyticsTab.DAILY -> {
                            state.dailyAnalytics?.let { data ->
                                DailyAnalyticsContent(data, state)
                            } ?: SkeletonAnalytics()
                        }
                        AnalyticsTab.WEEKLY -> {
                            state.weeklyAnalytics?.let { data ->
                                WeeklyAnalyticsContent(data, state)
                            } ?: SkeletonAnalytics()
                        }
                        AnalyticsTab.MONTHLY -> {
                            state.monthlyAnalytics?.let { data ->
                                MonthlyAnalyticsContent(data, state)
                            } ?: SkeletonAnalytics()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Period navigation bar allowing easy day/week/month navigation and jumping to current.
 */
@Composable
private fun PeriodNavigationBar(
    selectedTab: AnalyticsTab,
    selectedDate: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onResetToday: () -> Unit
) {
    val isCurrent = remember(selectedTab, selectedDate) {
        val today = LocalDate.now()
        when (selectedTab) {
            AnalyticsTab.DAILY -> selectedDate == today
            AnalyticsTab.WEEKLY -> {
                val weekStart = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
                val currentWeekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
                weekStart == currentWeekStart
            }
            AnalyticsTab.MONTHLY -> selectedDate.year == today.year && selectedDate.month == today.month
        }
    }

    val periodLabel = remember(selectedTab, selectedDate) {
        when (selectedTab) {
            AnalyticsTab.DAILY -> formatDateLabel(selectedDate)
            AnalyticsTab.WEEKLY -> formatWeekLabel(selectedDate)
            AnalyticsTab.MONTHLY -> formatMonthLabel(selectedDate)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.small, vertical = spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Kỳ trước")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!isCurrent) {
                IconButton(
                    onClick = onResetToday,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Về hiện tại",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Kỳ sau")
        }
    }
}

// ==========================================
// DAILY ANALYTICS CONTENT
// ==========================================

@Composable
fun DailyAnalyticsContent(data: DailyAnalytics, state: AnalyticsUiState) {
    val hasData = data.trackedDuration.inWholeMilliseconds > 0

    if (!hasData) {
        AnalyticsEmptyStateCard(message = "Chưa có hoạt động nào được ghi nhận trong ngày này.")
        return
    }

    // 1. Overview Summary Cards (Tracked, Unique, Overlap)
    TimeSummaryHeroCard(
        title = "Tổng quan Ngày",
        uniqueDuration = data.uniqueClockDuration,
        trackedDuration = data.trackedDuration,
        overlapDuration = data.overlapDuration
    )

    // 2. Concurrency Distribution
    ConcurrencyDistributionCard(
        concurrencyDistribution = data.concurrencyDistribution,
        uniqueClockDuration = data.uniqueClockDuration
    )

    // 3. Activity Breakdown
    ActivityBreakdownCard(
        activityDurations = data.activityDurations,
        totalDuration = data.trackedDuration,
        activities = state.activities
    )

    // 4. Activity Relationships (Multitasking)
    ActivityRelationshipsCard(
        relationships = data.activityRelationships,
        activities = state.activities
    )
}

// ==========================================
// WEEKLY ANALYTICS CONTENT
// ==========================================

@Composable
fun WeeklyAnalyticsContent(data: WeeklyAnalytics, state: AnalyticsUiState) {
    val hasData = data.totalTrackedDuration.inWholeMilliseconds > 0

    if (!hasData) {
        AnalyticsEmptyStateCard(message = "Chưa có dữ liệu ghi nhận nào trong tuần này.")
        return
    }

    val overlapDuration = (data.totalTrackedDuration - data.totalUniqueDuration).coerceAtLeast(Duration.ZERO)

    // 1. Overview Summary Cards
    TimeSummaryHeroCard(
        title = "Tổng quan Tuần",
        uniqueDuration = data.totalUniqueDuration,
        trackedDuration = data.totalTrackedDuration,
        overlapDuration = overlapDuration,
        averagePerDay = data.totalTrackedDuration / 7
    )

    // 2. Weekly Trend Bar Chart
    LogAppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Xu hướng 7 Ngày (Weekly Trend)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Thời lượng ghi nhận theo từng ngày trong tuần",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(spacing.medium))

        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            WeeklyBarChart(
                dailyBreakdown = data.dailyBreakdown,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // 3. Activity Breakdown
    ActivityBreakdownCard(
        activityDurations = data.activityDurations,
        totalDuration = data.totalTrackedDuration,
        activities = state.activities
    )

    // 4. Activity Relationships (Multitasking)
    ActivityRelationshipsCard(
        relationships = data.activityRelationships,
        activities = state.activities
    )
}

// ==========================================
// MONTHLY ANALYTICS CONTENT
// ==========================================

@Composable
fun MonthlyAnalyticsContent(data: MonthlyAnalytics, state: AnalyticsUiState) {
    val hasData = data.totalTrackedDuration.inWholeMilliseconds > 0

    if (!hasData) {
        AnalyticsEmptyStateCard(message = "Chưa có dữ liệu ghi nhận nào trong tháng này.")
        return
    }

    val overlapDuration = (data.totalTrackedDuration - data.totalUniqueDuration).coerceAtLeast(Duration.ZERO)
    val daysInMonth = data.yearMonth.lengthOfMonth()

    // 1. Overview Summary Cards
    TimeSummaryHeroCard(
        title = "Tổng quan Tháng",
        uniqueDuration = data.totalUniqueDuration,
        trackedDuration = data.totalTrackedDuration,
        overlapDuration = overlapDuration,
        averagePerDay = data.totalTrackedDuration / daysInMonth
    )

    // 2. Monthly Heatmap
    LogAppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Bản đồ Nhiệt Tháng (Monthly Heatmap)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Mức độ hoạt động theo từng ngày trong tháng",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(spacing.medium))

        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            MonthlyHeatmap(
                yearMonth = data.yearMonth,
                dailyBreakdown = data.dailyBreakdown,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // 3. Activity Breakdown
    ActivityBreakdownCard(
        activityDurations = data.activityDurations,
        totalDuration = data.totalTrackedDuration,
        activities = state.activities
    )

    // 4. Activity Relationships (Multitasking)
    ActivityRelationshipsCard(
        relationships = data.activityRelationships,
        activities = state.activities
    )
}

// ==========================================
// REUSABLE PRESENTATIONAL COMPONENTS
// ==========================================

/**
 * Top Summary Hero Card showing Unique Time, Tracked Time, Overlap Time, and RingChart.
 */
@Composable
private fun TimeSummaryHeroCard(
    title: String,
    uniqueDuration: Duration,
    trackedDuration: Duration,
    overlapDuration: Duration,
    averagePerDay: Duration? = null
) {
    LogAppElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tóm lược các chỉ số thời gian chính",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (overlapDuration.inWholeMinutes > 0) {
                LogAppStatusBadge(
                    status = StatusType.WARNING,
                    text = "Đa nhiệm",
                    icon = Icons.Default.Warning
                )
            } else {
                LogAppStatusBadge(
                    status = StatusType.SUCCESS,
                    text = "Đơn nhiệm"
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // RingChart & Hero metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                RingChart(
                    trackedDuration = trackedDuration.inWholeMilliseconds,
                    uniqueDuration = uniqueDuration.inWholeMilliseconds,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .padding(start = spacing.small),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                MetricRow(
                    label = "Thời gian thực (Unique)",
                    value = formatDurationFriendly(uniqueDuration),
                    color = MaterialTheme.colorScheme.primary,
                    caption = "Thời gian trôi qua thực tế"
                )
                MetricRow(
                    label = "Tổng ghi nhận (Tracked)",
                    value = formatDurationFriendly(trackedDuration),
                    color = MaterialTheme.colorScheme.secondary,
                    caption = "Tích lũy các phiên"
                )
                MetricRow(
                    label = "Trùng lặp (Overlap)",
                    value = formatDurationFriendly(overlapDuration),
                    color = if (overlapDuration.inWholeMinutes > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    caption = "Thời gian làm song song"
                )
            }
        }

        if (averagePerDay != null) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = spacing.small),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trung bình mỗi ngày:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDurationFriendly(averagePerDay),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    color: Color,
    caption: String
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}

/**
 * Concurrency Distribution breakdown card (Single-task vs Multi-task levels).
 */
@Composable
private fun ConcurrencyDistributionCard(
    concurrencyDistribution: Map<Int, Duration>,
    uniqueClockDuration: Duration
) {
    LogAppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Phân bổ Mức độ Đồng thời (Concurrency)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Mức độ tập trung đơn nhiệm so với làm việc đa nhiệm",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(spacing.medium))

        if (concurrencyDistribution.isEmpty()) {
            Text(
                text = "Chưa có dữ liệu phân bổ.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                concurrencyDistribution.entries.sortedBy { it.key }.forEach { (level, duration) ->
                    val fraction = if (uniqueClockDuration.inWholeMilliseconds > 0) {
                        duration.inWholeMilliseconds.toFloat() / uniqueClockDuration.inWholeMilliseconds
                    } else 0f
                    val percentage = (fraction * 100).toInt()

                    val levelName = when (level) {
                        1 -> "Mức 1: Đơn nhiệm (1 việc)"
                        2 -> "Mức 2: Song song 2 việc"
                        else -> "Mức $level: Song song $level việc"
                    }
                    val levelColor = if (level == 1) MaterialTheme.colorScheme.primary else Color(0xFFFF9800)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = levelName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${formatDurationFriendly(duration)} ($percentage%)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = levelColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = levelColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Activity Breakdown card with custom color progress bars and percentages.
 */
@Composable
private fun ActivityBreakdownCard(
    activityDurations: Map<String, Duration>,
    totalDuration: Duration,
    activities: List<ActivityEntity>
) {
    LogAppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Phân bổ theo Hoạt động",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Xếp hạng hoạt động chiếm nhiều thời gian nhất",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${activityDurations.size} hoạt động",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        if (activityDurations.isEmpty()) {
            Text(
                text = "Chưa có hoạt động nào trong kỳ này.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                activityDurations.entries.sortedByDescending { it.value }.forEach { (activityId, duration) ->
                    val activity = activities.find { it.id == activityId }
                    val color = activity?.color?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.primary
                    val fraction = if (totalDuration.inWholeMilliseconds > 0) {
                        duration.inWholeMilliseconds.toFloat() / totalDuration.inWholeMilliseconds
                    } else 0f
                    val percentage = (fraction * 100).toInt()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(
                                    text = activity?.name ?: "Chưa đặt tên",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = formatDurationFriendly(duration),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = color,
                            trackColor = color.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Activity Relationships card separating Multitasking analysis from raw data.
 */
@Composable
private fun ActivityRelationshipsCard(
    relationships: Map<Pair<String, String>, Duration>,
    activities: List<ActivityEntity>
) {
    LogAppCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Hoạt động Song song (Relationships)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Các cặp hoạt động bạn thường xuyên thực hiện cùng một lúc",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        RelationshipStats(
            relationships = relationships,
            activities = activities,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Empty state card when there are no analytics in the current period.
 */
@Composable
private fun AnalyticsEmptyStateCard(message: String) {
    LogAppEmptyState(
        icon = Icons.Default.Info,
        title = "Chưa có dữ liệu phân tích",
        message = message,
        modifier = Modifier.padding(vertical = spacing.small)
    )
}

// ==========================================
// FORMATTING HELPERS
// ==========================================

private fun formatDateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Hôm nay (${date.format(DateTimeFormatter.ofPattern("dd/MM"))})"
        today.minusDays(1) -> "Hôm qua (${date.format(DateTimeFormatter.ofPattern("dd/MM"))})"
        today.plusDays(1) -> "Ngày mai (${date.format(DateTimeFormatter.ofPattern("dd/MM"))})"
        else -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
}

private fun formatWeekLabel(date: LocalDate): String {
    val weekStart = date.minusDays((date.dayOfWeek.value - 1).toLong())
    val weekEnd = weekStart.plusDays(6)
    val startStr = weekStart.format(DateTimeFormatter.ofPattern("dd/MM"))
    val endStr = weekEnd.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    return "Tuần $startStr - $endStr"
}

private fun formatMonthLabel(date: LocalDate): String {
    return "Tháng ${date.format(DateTimeFormatter.ofPattern("MM/yyyy"))}"
}

private fun formatDurationFriendly(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    if (totalMinutes <= 0) return "0p"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        if (minutes > 0) "${hours}h ${minutes}p" else "${hours}h"
    } else {
        "${minutes}p"
    }
}
