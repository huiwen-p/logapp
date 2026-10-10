package com.example.logapp.feature.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logapp.domain.engine.DailyAnalytics
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthlyHeatmap(
    yearMonth: YearMonth,
    dailyBreakdown: Map<LocalDate, DailyAnalytics>,
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.primary
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    // Get max duration for scaling color intensity
    val maxDuration = dailyBreakdown.values.maxOfOrNull { it.trackedDuration.inWholeMinutes } ?: 1L
    val safeMax = if (maxDuration == 0L) 1L else maxDuration

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val cols = 7 // Mon to Sun
        val rows = 6 // Up to 6 weeks in a month
        
        // Calculate cell size and padding
        val padding = 4.dp.toPx()
        val cellWidth = (width - padding * (cols - 1)) / cols
        val cellHeight = (height - 30f - padding * (rows - 1)) / rows
        val cellSize = minOf(cellWidth, cellHeight)
        
        // Centering offset
        val startX = (width - (cellSize * cols + padding * (cols - 1))) / 2f
        val startY = 0f
        
        val firstDayOfMonth = yearMonth.atDay(1)
        // Adjust for Monday = 1, Sunday = 7
        val firstDayOffset = firstDayOfMonth.dayOfWeek.value - 1
        
        val daysInMonth = yearMonth.lengthOfMonth()
        
        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val daily = dailyBreakdown[date]
            
            val durationMinutes = daily?.trackedDuration?.inWholeMinutes ?: 0L
            val fraction = durationMinutes.toFloat() / safeMax
            
            // Color mapping: if 0 then surface, else blend from 20% to 100% of baseColor
            val cellColor = if (durationMinutes > 0L) {
                baseColor.copy(alpha = 0.2f + (0.8f * fraction))
            } else {
                surfaceColor.copy(alpha = 0.5f)
            }
            
            val gridIndex = firstDayOffset + day - 1
            val col = gridIndex % 7
            val row = gridIndex / 7
            
            val x = startX + col * (cellSize + padding)
            val y = startY + row * (cellSize + padding)
            
            drawRoundRect(
                color = cellColor,
                topLeft = Offset(x, y),
                size = Size(cellSize, cellSize),
                cornerRadius = CornerRadius(cellSize / 4, cellSize / 4)
            )
            
            // Draw day number if cell size is large enough
            if (cellSize > 40f) {
                val textLayoutResult = textMeasurer.measure(
                    day.toString(), 
                    TextStyle(
                        fontSize = 10.sp, 
                        color = if (durationMinutes > maxDuration / 2) Color.White else onSurfaceColor
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x + (cellSize - textLayoutResult.size.width) / 2, 
                        y + (cellSize - textLayoutResult.size.height) / 2
                    )
                )
            }
        }
        
        // Draw day of week labels at the bottom
        val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
        daysOfWeek.forEachIndexed { col, label ->
            val textLayoutResult = textMeasurer.measure(label, TextStyle(fontSize = 12.sp, color = onSurfaceColor))
            val x = startX + col * (cellSize + padding) + (cellSize - textLayoutResult.size.width) / 2
            val y = height - 20f
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(x, y)
            )
        }
    }
}
