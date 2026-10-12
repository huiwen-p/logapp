package com.example.logapp.feature.timeline.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.feature.timeline.engine.TimelineLayoutEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TimelineCanvas(
    selectedDate: LocalDate,
    sessions: List<ActivitySessionEntity>,
    activities: List<ActivityEntity>,
    onSessionClick: (ActivitySessionEntity) -> Unit,
    onSessionLongClick: ((ActivitySessionEntity) -> Unit)? = null,
    selectedSessionIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    var scaleY by remember { mutableFloatStateOf(1.5f) }
    var offsetY by remember { mutableFloatStateOf(-500f) } // Initial offset to start a bit down

    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val nowLineColor = Color(0xFFE53935) // Red indicator
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // Pre-measure hour labels to optimize rendering performance and prevent negative constraint crashes
    val hourLabels = remember(textMeasurer, onSurfaceColor) {
        (0..24).map { hour ->
            textMeasurer.measure(
                text = String.format("%02d:00", hour),
                style = TextStyle(color = onSurfaceColor, fontSize = 12.sp)
            )
        }
    }

    // Pre-measure static badges to eliminate text measurement inside 60fps draw loop
    val prevDayBadge = remember(textMeasurer) {
        textMeasurer.measure(
            text = "↰ từ hôm trước",
            style = TextStyle(
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
    val nextDayBadge = remember(textMeasurer) {
        textMeasurer.measure(
            text = "sang hôm sau 🌙",
            style = TextStyle(
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
    val overlapSymbol = remember(textMeasurer) {
        textMeasurer.measure(
            text = "⇄",
            style = TextStyle(
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }

    // Memoize activities map for O(1) lookup during drawing
    val activityMap = remember(activities) {
        activities.associateBy { it.id }
    }

    // Memory cache for session title layouts to eliminate allocations during continuous pan/zoom
    val textLayoutCache = remember { mutableMapOf<Triple<String, Int, Boolean>, androidx.compose.ui.text.TextLayoutResult>() }
    LaunchedEffect(activities) {
        textLayoutCache.clear()
    }

    // Plain mutable list for gesture hit testing (avoids SnapshotStateList overhead in DrawScope)
    val renderedBlocks = remember { mutableListOf<Pair<Rect, ActivitySessionEntity>>() }

    // Cluster-based Overlap layout logic: Isolated sessions get 100% width, overlapping clusters divide width
    val layoutMap = remember(sessions) {
        TimelineLayoutEngine.calculateLayout(sessions)
    }
    val sortedSessions = remember(sessions) {
        sessions.sortedBy { it.startedAt }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scaleY = (scaleY * zoom).coerceIn(0.5f, 5f)
                    val canvasHeight = size.height
                    val timelineHeight = canvasHeight * scaleY
                    // Prevent scrolling completely out of bounds
                    val maxOffset = 50f
                    val minOffset = -(timelineHeight - canvasHeight + 50f).coerceAtLeast(0f)
                    
                    offsetY = (offsetY + pan.y).coerceIn(minOffset, maxOffset)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val clickedPair = renderedBlocks.find { it.first.contains(tapOffset) }
                        if (clickedPair != null) {
                            onSessionClick(clickedPair.second)
                        }
                    },
                    onLongPress = { tapOffset ->
                        val clickedPair = renderedBlocks.find { it.first.contains(tapOffset) }
                        if (clickedPair != null && onSessionLongClick != null) {
                            onSessionLongClick(clickedPair.second)
                        }
                    }
                )
            }
    ) {
        renderedBlocks.clear()
        
        val canvasWidth = size.width
        val canvasHeight = size.height

        val now = Instant.now()
        val isToday = selectedDate == LocalDate.now(ZoneId.systemDefault())
        val startOfDayMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        // Draw Time Axis (left side)
        val timeAxisWidth = 60.dp.toPx()
        
        // Draw 30 minute grids
        for (i in 0..48) {
            val timeMillis = startOfDayMillis + (i * 30 * 60 * 1000L)
            val y = ((timeMillis - startOfDayMillis).toFloat() / oneDayMillis) * canvasHeight * scaleY + offsetY
            
            if (y in -50f..(canvasHeight + 50f)) {
                if (i % 2 == 0) {
                    // Full hour
                    drawLine(
                        color = lineColor,
                        start = Offset(timeAxisWidth, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    val textLayoutResult = hourLabels.getOrNull(i / 2)
                    if (textLayoutResult != null) {
                        val textTopLeftY = y - (textLayoutResult.size.height / 2f)
                        if (textTopLeftY in -textLayoutResult.size.height.toFloat()..canvasHeight) {
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(10f, textTopLeftY)
                            )
                        }
                    }
                } else {
                    // Half hour
                    drawLine(
                        color = lineColor,
                        start = Offset(timeAxisWidth, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }
        }

        // Draw Current Time Line
        if (isToday) {
            val nowY = ((now.toEpochMilli() - startOfDayMillis).toFloat() / oneDayMillis) * canvasHeight * scaleY + offsetY
            if (nowY in -50f..(canvasHeight + 50f)) {
                drawLine(
                    color = nowLineColor,
                    start = Offset(timeAxisWidth, nowY),
                    end = Offset(canvasWidth, nowY),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = nowLineColor,
                    radius = 4.dp.toPx(),
                    center = Offset(timeAxisWidth, nowY)
                )
            }
        }

        // Draw Blocks
        clipRect(left = timeAxisWidth, top = 0f, right = canvasWidth, bottom = canvasHeight) {
            sortedSessions.forEach { session ->
                val activity = activityMap[session.activityId]
                val color = activity?.color?.let { Color(it.toInt()) } ?: Color(0xFF6750A4)

                val startMilli = maxOf(startOfDayMillis, session.startedAt.toEpochMilli())
                val endMilli = minOf(startOfDayMillis + oneDayMillis, session.endedAt?.toEpochMilli() ?: now.toEpochMilli())
                
                val startY = ((startMilli - startOfDayMillis).toFloat() / oneDayMillis) * canvasHeight * scaleY + offsetY
                val endY = ((endMilli - startOfDayMillis).toFloat() / oneDayMillis) * canvasHeight * scaleY + offsetY

                // Culling: Skip if block is entirely outside visible bounds
                if (endY < -50f || startY > canvasHeight + 50f) {
                    return@forEach
                }

                val height = (endY - startY).coerceAtLeast(18f)
                
                val layoutPos = layoutMap[session.id]
                val colIndex = layoutPos?.columnIndex ?: 0
                val totalCols = layoutPos?.totalColumns ?: 1
                val isOverlapping = layoutPos?.isOverlapping ?: false

                val availableWidth = canvasWidth - timeAxisWidth - 16f
                val colWidth = availableWidth / totalCols
                val x = timeAxisWidth + 8f + colIndex * colWidth
                val blockWidth = (colWidth - 4f).coerceAtLeast(16f)

                val startsFromPreviousDay = session.startedAt.toEpochMilli() < startOfDayMillis
                val continuesToNextDay = (session.endedAt?.toEpochMilli() ?: now.toEpochMilli()) > (startOfDayMillis + oneDayMillis)

                val topRadius = if (startsFromPreviousDay) 0f else 14f
                val bottomRadius = if (continuesToNextDay) 0f else 14f

                val rect = Rect(Offset(x, startY), Size(blockWidth, height))
                renderedBlocks.add(Pair(rect, session))

                // Block background
                drawRoundRect(
                    color = color.copy(alpha = 0.90f),
                    topLeft = Offset(x, startY),
                    size = Size(blockWidth, height),
                    cornerRadius = CornerRadius(topRadius, topRadius)
                )

                // Block subtle outline
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f),
                    topLeft = Offset(x, startY),
                    size = Size(blockWidth, height),
                    cornerRadius = CornerRadius(topRadius, topRadius),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Overnight continuation indicators (using pre-measured static badges)
                if (startsFromPreviousDay && blockWidth > 70f && height > 20f) {
                    drawText(
                        textLayoutResult = prevDayBadge,
                        topLeft = Offset(x + blockWidth - prevDayBadge.size.width - 6f, startY + 3f)
                    )
                }

                if (continuesToNextDay && blockWidth > 70f && height > 20f) {
                    val badgeY = startY + height - nextDayBadge.size.height - 3f
                    if (badgeY > startY) {
                        drawText(
                            textLayoutResult = nextDayBadge,
                            topLeft = Offset(x + blockWidth - nextDayBadge.size.width - 6f, badgeY)
                        )
                    }
                }

                // Visual cue for overlap: subtle dashed indicator + icon cue
                if (isOverlapping) {
                    // Left overlap indicator stripe
                    drawRoundRect(
                        color = Color(0xFFFFB74D), // Amber warm indicator
                        topLeft = Offset(x, startY),
                        size = Size(4.dp.toPx(), height),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Overlap visual cue symbol (⇄) in top right if block is large enough
                    if (blockWidth > 70f && height > 28f) {
                        val symbolX = x + blockWidth - overlapSymbol.size.width - 8f
                        val symbolY = startY + 6f
                        if (symbolX > x && symbolY + overlapSymbol.size.height < startY + height) {
                            drawText(
                                textLayoutResult = overlapSymbol,
                                topLeft = Offset(symbolX, symbolY)
                            )
                        }
                    }
                }

                // Selection Border
                if (selectedSessionIds.contains(session.id)) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(x, startY),
                        size = Size(blockWidth, height),
                        cornerRadius = CornerRadius(14f, 14f),
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                }

                // Content inside block
                val activityName = activity?.name.orEmpty()
                if (activityName.isNotEmpty() && height > 28f && blockWidth > 50f) {
                    val textTopLeftY = startY + 8f
                    val textTopLeftX = if (isOverlapping) x + 12f else x + 10f
                    val maxTextWidth = (blockWidth - (if (isOverlapping) 34f else 20f)).toInt().coerceAtLeast(0)

                    if (maxTextWidth > 0 && textTopLeftY in -40f..canvasHeight && textTopLeftX in 0f..canvasWidth) {
                        val isBig = height > 50f
                        val cacheKey = Triple(activityName, maxTextWidth, isBig)
                        val textLayoutResult = textLayoutCache.getOrPut(cacheKey) {
                            textMeasurer.measure(
                                text = activityName,
                                style = TextStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isBig) 13.sp else 11.sp
                                ),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                constraints = Constraints(maxWidth = maxTextWidth)
                            )
                        }
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(textTopLeftX, textTopLeftY)
                        )

                        // Draw time range (HH:mm - HH:mm) if block has enough vertical space
                        if (height >= 56f) {
                            val startLocal = session.startedAt.atZone(ZoneId.systemDefault()).toLocalTime()
                            val endLocal = (session.endedAt ?: now).atZone(ZoneId.systemDefault()).toLocalTime()
                            val timeStr = "${startLocal.format(timeFormatter)} - ${if (session.endedAt == null) "hiện tại" else endLocal.format(timeFormatter)}"

                            val timeTopLeftY = textTopLeftY + textLayoutResult.size.height + 2f
                            if (timeTopLeftY + 14f <= startY + height) {
                                val timeLayoutResult = textMeasurer.measure(
                                    text = timeStr,
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    constraints = Constraints(maxWidth = maxTextWidth)
                                )
                                drawText(
                                    textLayoutResult = timeLayoutResult,
                                    topLeft = Offset(textTopLeftX, timeTopLeftY)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
