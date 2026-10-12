package com.example.logapp.feature.timeline.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logapp.core.designsystem.component.LogAppCard
import com.example.logapp.core.designsystem.component.LogAppStatusBadge
import com.example.logapp.core.designsystem.component.StatusType
import com.example.logapp.core.designsystem.tokens.spacing
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.feature.timeline.engine.TimelineLayoutEngine
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * High-performance, virtualized LazyColumn session list view.
 *
 * Optimized for datasets with 1,000+ sessions:
 * - Stable item keys and contentTypes for recycled item composition.
 * - O(1) activity lookups via memoized map.
 * - Smooth scroll-to-top floating action button.
 * - Supports single-tap edit, long-press to enter multi-select merge mode.
 */
@Composable
fun SessionListLazyColumn(
    sessions: List<ActivitySessionEntity>,
    activities: List<ActivityEntity>,
    onSessionClick: (ActivitySessionEntity) -> Unit,
    onSessionLongClick: (ActivitySessionEntity) -> Unit,
    selectedSessionIds: Set<String> = emptySet(),
    isMultiSelectMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Memoize activities map for O(1) lookup across 1,000+ items
    val activityMap = remember(activities) {
        activities.associateBy { it.id }
    }

    // Pre-calculate layout & overlap state using TimelineLayoutEngine
    val layoutMap = remember(sessions) {
        TimelineLayoutEngine.calculateLayout(sessions)
    }

    // Sort chronologically (most recent or earliest first)
    val sortedSessions = remember(sessions) {
        sessions.sortedBy { it.startedAt }
    }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = spacing.medium,
                end = spacing.medium,
                top = spacing.small,
                bottom = 88.dp // Space for bottom FAB and nav
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = sortedSessions,
                key = { it.id },
                contentType = { "session_card" }
            ) { session ->
                val activity = activityMap[session.activityId]
                val layoutPos = layoutMap[session.id]
                val isOverlapping = layoutPos?.isOverlapping ?: false
                val isSelected = selectedSessionIds.contains(session.id)

                SessionListItemCard(
                    session = session,
                    activity = activity,
                    isOverlapping = isOverlapping,
                    isSelected = isSelected,
                    isMultiSelectMode = isMultiSelectMode,
                    timeFormatter = timeFormatter,
                    onClick = { onSessionClick(session) },
                    onLongClick = { onSessionLongClick(session) }
                )
            }
        }

        // Fast Scroll to Top Floating Indicator when list is scrolled down
        val showScrollToTop by remember {
            derivedStateOf { listState.firstVisibleItemIndex > 5 }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = spacing.medium, bottom = 80.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Cuộn lên đầu"
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SessionListItemCard(
    session: ActivitySessionEntity,
    activity: ActivityEntity?,
    isOverlapping: Boolean,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val startLocal = remember(session.startedAt) { session.startedAt.atZone(zone).toLocalTime() }
    val isOngoing = session.endedAt == null
    val endLocal = remember(session.endedAt) {
        if (session.endedAt != null) session.endedAt.atZone(zone).toLocalTime() else null
    }

    val duration = remember(session.startedAt, session.endedAt) {
        val endInstant = session.endedAt ?: Instant.now()
        Duration.between(session.startedAt, endInstant)
    }

    val durationStr = remember(duration) {
        val totalMins = duration.toMinutes()
        val hours = totalMins / 60
        val mins = totalMins % 60
        if (hours > 0) "${hours}h ${mins}p" else "${mins}p"
    }

    val activityColor = remember(activity?.color) {
        activity?.color?.let { Color(it.toInt()) } ?: Color(0xFF6750A4)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                isOngoing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else if (isOngoing) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Accent Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activityColor)
            )

            Spacer(modifier = Modifier.width(spacing.medium))

            // Activity Icon or First Letter
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(activityColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!activity?.icon.isNullOrBlank()) {
                    Text(text = activity?.icon ?: "", fontSize = 20.sp)
                } else {
                    Text(
                        text = activity?.name?.take(1)?.uppercase() ?: "A",
                        fontWeight = FontWeight.Bold,
                        color = activityColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.width(spacing.medium))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    Text(
                        text = activity?.name ?: "Chưa xác định",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isOngoing) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Time Range & Duration
                val timeRangeStr = if (isOngoing) {
                    "${startLocal.format(timeFormatter)} - Đang chạy"
                } else {
                    "${startLocal.format(timeFormatter)} - ${endLocal?.format(timeFormatter)}"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Text(
                        text = timeRangeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = durationStr,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Session Note if present
                if (!session.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right side indicators: Overlap Badge / Selection Checkbox
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                if (isMultiSelectMode) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Đã chọn",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                } else if (isOverlapping) {
                    LogAppStatusBadge(
                        status = StatusType.WARNING,
                        text = "Trùng lặp",
                        icon = Icons.Default.Warning
                    )
                }
            }
        }
    }
}
