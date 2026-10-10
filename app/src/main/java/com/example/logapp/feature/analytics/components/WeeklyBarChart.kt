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

@Composable
fun WeeklyBarChart(
    dailyBreakdown: Map<LocalDate, DailyAnalytics>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Get max duration for scaling
    val maxDuration = dailyBreakdown.values.maxOfOrNull { it.trackedDuration.inWholeMinutes } ?: 1L
    val safeMax = if (maxDuration == 0L) 1L else maxDuration

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val barWidth = width / 14f
        val spacing = barWidth
        
        val sortedEntries = dailyBreakdown.entries.sortedBy { it.key }
        
        sortedEntries.forEachIndexed { index, entry ->
            val date = entry.key
            val daily = entry.value
            
            val durationMinutes = daily.trackedDuration.inWholeMinutes
            val fraction = durationMinutes.toFloat() / safeMax
            
            val barHeight = (height - 40f) * fraction
            val x = (spacing * 0.5f) + index * (barWidth + spacing)
            val y = height - 30f - barHeight
            
            // Draw bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
            
            // Draw day label (e.g. "Mon")
            val dayName = date.dayOfWeek.name.take(3)
            val textLayoutResult = textMeasurer.measure(dayName, TextStyle(fontSize = 12.sp, color = onSurfaceColor))
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(x + (barWidth - textLayoutResult.size.width) / 2, height - 20f)
            )
        }
    }
}
