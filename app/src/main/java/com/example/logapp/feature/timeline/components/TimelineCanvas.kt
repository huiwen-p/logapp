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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TimelineCanvas(
    sessions: List<ActivitySessionEntity>,
    activities: List<ActivityEntity>,
    onSessionClick: (ActivitySessionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var scaleY by remember { mutableFloatStateOf(1f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    // To store rendered rectangles for tap detection
    val renderedBlocks = remember { mutableStateListOf<Pair<Rect, ActivitySessionEntity>>() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scaleY = (scaleY * zoom).coerceIn(0.5f, 5f)
                    offsetY += pan.y
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val clickedPair = renderedBlocks.find { it.first.contains(tapOffset) }
                    if (clickedPair != null) {
                        onSessionClick(clickedPair.second)
                    }
                }
            }
    ) {
        renderedBlocks.clear()
        
        val canvasWidth = size.width
        val canvasHeight = size.height

        val now = Instant.now().toEpochMilli()
        val oneDay = 24 * 60 * 60 * 1000L
        val startOfDay = now - (now % oneDay) - (7 * 60 * 60 * 1000L) // Adjust to local roughly

        // Draw Time Axis (left side)
        val timeAxisWidth = 60.dp.toPx()
        for (i in 0..24) {
            val timeMillis = startOfDay + (i * 60 * 60 * 1000L)
            val y = ((timeMillis - startOfDay).toFloat() / oneDay) * canvasHeight * scaleY + offsetY
            
            if (y in 0f..canvasHeight) {
                drawLine(
                    color = lineColor,
                    start = Offset(timeAxisWidth, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = String.format("%02d:00", i),
                    topLeft = Offset(10f, y - 24f),
                    style = TextStyle(color = onSurfaceColor, fontSize = 12.sp)
                )
            }
        }

        // Draw Blocks
        clipRect(left = timeAxisWidth, top = 0f, right = canvasWidth, bottom = canvasHeight) {
            sessions.forEachIndexed { index, session ->
                val activity = activities.find { it.id == session.activityId }
                val color = activity?.color?.let { Color(it) } ?: Color.Gray

                val startY = ((session.startedAt.toEpochMilli() - startOfDay).toFloat() / oneDay) * canvasHeight * scaleY + offsetY
                val endMilli = session.endedAt?.toEpochMilli() ?: now
                val endY = ((endMilli - startOfDay).toFloat() / oneDay) * canvasHeight * scaleY + offsetY

                val height = (endY - startY).coerceAtLeast(20f)
                val width = (canvasWidth - timeAxisWidth) * 0.45f
                
                // Simple overlap offset logic
                val x = timeAxisWidth + 20f + (index % 2).toFloat() * width * 1.1f

                val rect = Rect(Offset(x, startY), Size(width, height))
                renderedBlocks.add(Pair(rect, session))

                drawRoundRect(
                    color = color.copy(alpha = 0.85f),
                    topLeft = Offset(x, startY),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Draw Activity Name inside block if height allows
                if (height > 40f) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = activity?.name ?: "",
                        topLeft = Offset(x + 16f, startY + 16f),
                        style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )
                }
            }
        }
    }
}
