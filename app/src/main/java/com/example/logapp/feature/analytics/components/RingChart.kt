package com.example.logapp.feature.analytics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RingChart(
    trackedDuration: Long,
    uniqueDuration: Long,
    modifier: Modifier = Modifier
) {
    val overlapDuration = (trackedDuration - uniqueDuration).coerceAtLeast(0)
    val total = trackedDuration.coerceAtLeast(1) // Avoid division by zero
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val overlapColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(trackedDuration, uniqueDuration) {
        animationProgress.animateTo(1f, animationSpec = tween(1000))
    }

    Box(
        modifier = modifier
            .padding(16.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.width * 0.15f
            val innerRadius = (size.minDimension - strokeWidth) / 2
            val centerOffset = Offset(size.width / 2, size.height / 2)

            // Draw background track
            drawCircle(
                color = trackColor,
                radius = innerRadius,
                center = centerOffset,
                style = Stroke(width = strokeWidth)
            )

            // Draw Tracked Time (Full ring scaled by animation)
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * animationProgress.value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(centerOffset.x - innerRadius, centerOffset.y - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2)
            )

            // Draw Overlap Time
            val overlapSweep = (overlapDuration.toFloat() / total) * 360f * animationProgress.value
            drawArc(
                color = overlapColor,
                startAngle = -90f,
                sweepAngle = overlapSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(centerOffset.x - innerRadius, centerOffset.y - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2)
            )
        }

        // Text inside ring
        val percentage = if (trackedDuration > 0) (uniqueDuration.toFloat() / trackedDuration * 100).toInt() else 0
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
