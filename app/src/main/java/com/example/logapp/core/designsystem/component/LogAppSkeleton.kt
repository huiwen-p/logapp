package com.example.logapp.core.designsystem.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.logapp.core.designsystem.tokens.spacing

/**
 * Shimmer effect modifier that smoothly sweeps across the component.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val highlightColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim + 300f, translateAnim + 300f)
    )

    background(shimmerBrush)
}

/**
 * Generic skeleton placeholder box.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmerEffect()
    )
}

/**
 * Skeleton activity grid used for Home Screen and Activity Selection.
 */
@Composable
fun SkeletonActivityGrid(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
    columns: Int = 3
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
        modifier = modifier.fillMaxWidth()
    ) {
        items(itemCount) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * Skeleton placeholder for Analytics Screen.
 */
@Composable
fun SkeletonAnalytics(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium, vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        // Hero Card Skeleton
        LogAppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(modifier = Modifier.width(140.dp).height(24.dp))
                SkeletonBox(modifier = Modifier.size(36.dp), shape = CircleShape)
            }
            Spacer(modifier = Modifier.height(spacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    SkeletonBox(modifier = Modifier.width(120.dp).height(16.dp))
                    SkeletonBox(modifier = Modifier.width(160.dp).height(32.dp))
                    SkeletonBox(modifier = Modifier.width(100.dp).height(14.dp))
                }
                SkeletonBox(modifier = Modifier.size(80.dp), shape = CircleShape)
            }
        }

        // Concurrency Distribution Skeleton
        LogAppCard(modifier = Modifier.fillMaxWidth()) {
            SkeletonBox(modifier = Modifier.width(180.dp).height(20.dp))
            Spacer(modifier = Modifier.height(spacing.medium))
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(16.dp), shape = RoundedCornerShape(8.dp))
            Spacer(modifier = Modifier.height(spacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SkeletonBox(modifier = Modifier.width(100.dp).height(16.dp))
                SkeletonBox(modifier = Modifier.width(100.dp).height(16.dp))
            }
        }

        // Activity Breakdown Skeleton
        LogAppCard(modifier = Modifier.fillMaxWidth()) {
            SkeletonBox(modifier = Modifier.width(160.dp).height(20.dp))
            Spacer(modifier = Modifier.height(spacing.medium))
            repeat(3) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.small),
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SkeletonBox(modifier = Modifier.width(120.dp).height(16.dp))
                        SkeletonBox(modifier = Modifier.width(60.dp).height(16.dp))
                    }
                    SkeletonBox(
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Skeleton placeholder for Timeline Screen.
 */
@Composable
fun SkeletonTimeline(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Summary bar skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonBox(modifier = Modifier.width(150.dp).height(24.dp))
            SkeletonBox(modifier = Modifier.width(100.dp).height(24.dp))
        }

        // Canvas / block skeletons
        repeat(4) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

/**
 * Skeleton placeholder for activity list in Manage Activities Screen.
 */
@Composable
fun SkeletonActivityList(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        repeat(itemCount) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
