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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    // To store rendered rectangles for tap detection
    val renderedBlocks = remember { mutableStateListOf<Pair<Rect, ActivitySessionEntity>>() }

    // Overlap layout logic: Sort sessions by start time
    val sortedSessions = sessions.sortedBy { it.startedAt }
    
    // Group overlapping sessions to calculate columns
    val sessionColumns = mutableMapOf<String, Int>()
    val columns = mutableListOf<MutableList<ActivitySessionEntity>>()
    
    for (session in sortedSessions) {
        var placed = false
        for (i in 0 until columns.size) {
            val lastInCol = columns[i].last()
            val lastEnd = lastInCol.endedAt ?: Instant.now()
            if (!session.startedAt.isBefore(lastEnd)) {
                columns[i].add(session)
                sessionColumns[session.id] = i
                placed = true
                break
            }
        }
        if (!placed) {
            columns.add(mutableListOf(session))
            sessionColumns[session.id] = columns.size - 1
        }
    }
    
    val totalColumns = maxOf(1, columns.size)

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
                    val textTopLeftY = y - 24f
                    if (textTopLeftY < canvasHeight && textTopLeftY > -50f) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = String.format("%02d:00", i / 2),
                            topLeft = Offset(10f, textTopLeftY),
                            style = TextStyle(color = onSurfaceColor, fontSize = 12.sp)
                        )
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
                val activity = activities.find { it.id == session.activityId }
                val color = activity?.color?.let { Color(it.toInt()) } ?: Color.Gray

                val startMilli = maxOf(startOfDayMillis, session.startedAt.toEpochMilli())
                val endMilli = minOf(startOfDayMillis + oneDayMillis, session.endedAt?.toEpochMilli() ?: now.toEpochMilli())
                
                val startY = ((startMilli - startOfDayMillis).toFloat() / oneDayMillis) * canvasHeight * scaleY + offsetY
                val endY = ((endMilli - startOfDayMillis).toFloat() / oneDayMillis) * canvasHeight * scaleY + offsetY

                // Culling: Skip if block is entirely outside visible bounds
                if (endY < -100f || startY > canvasHeight + 100f) {
                    return@forEach
                }

                val height = (endY - startY).coerceAtLeast(16f)
                
                val colIndex = sessionColumns[session.id] ?: 0
                val colWidth = (canvasWidth - timeAxisWidth - 16f) / totalColumns
                val x = timeAxisWidth + 8f + colIndex * colWidth

                val rect = Rect(Offset(x, startY), Size(colWidth - 4f, height))
                renderedBlocks.add(Pair(rect, session))

                drawRoundRect(
                    color = color.copy(alpha = 0.85f),
                    topLeft = Offset(x, startY),
                    size = Size(colWidth - 4f, height),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                if (selectedSessionIds.contains(session.id)) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(x, startY),
                        size = Size(colWidth - 4f, height),
                        cornerRadius = CornerRadius(16f, 16f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                    )
                }

                // Draw Activity Name inside block if height allows and it's within visible bounds
                if (height > 40f && colWidth > 100f) {
                    val textTopLeftY = startY + 16f
                    val textTopLeftX = x + 16f
                    if (textTopLeftY < canvasHeight && textTopLeftX < canvasWidth) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = activity?.name ?: "",
                            topLeft = Offset(textTopLeftX, textTopLeftY),
                            style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        )
                    }
                }
            }
        }
    }
}
