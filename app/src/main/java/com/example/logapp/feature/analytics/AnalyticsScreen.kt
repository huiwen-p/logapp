package com.example.logapp.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logapp.feature.analytics.components.RingChart
import com.example.logapp.feature.analytics.components.WeeklyBarChart
import com.example.logapp.feature.analytics.components.MonthlyHeatmap
import com.example.logapp.feature.analytics.components.RelationshipStats
import com.example.logapp.domain.engine.DailyAnalytics
import com.example.logapp.domain.engine.WeeklyAnalytics
import com.example.logapp.domain.engine.MonthlyAnalytics
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
                title = { Text("Analytics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displayMedium) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                Tab(
                    selected = state.selectedTab == AnalyticsTab.DAILY,
                    onClick = { viewModel.setTab(AnalyticsTab.DAILY) },
                    text = { Text("Daily") }
                )
                Tab(
                    selected = state.selectedTab == AnalyticsTab.WEEKLY,
                    onClick = { viewModel.setTab(AnalyticsTab.WEEKLY) },
                    text = { Text("Weekly") }
                )
                Tab(
                    selected = state.selectedTab == AnalyticsTab.MONTHLY,
                    onClick = { viewModel.setTab(AnalyticsTab.MONTHLY) },
                    text = { Text("Monthly") }
                )
            }
            
            if (state.selectedTab == AnalyticsTab.DAILY) {
                state.dailyAnalytics?.let { data ->
                    DailyAnalyticsContent(data, state)
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.selectedTab == AnalyticsTab.WEEKLY) {
                state.weeklyAnalytics?.let { data ->
                    WeeklyAnalyticsContent(data, state)
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.selectedTab == AnalyticsTab.MONTHLY) {
                state.monthlyAnalytics?.let { data ->
                    MonthlyAnalyticsContent(data, state)
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun DailyAnalyticsContent(data: DailyAnalytics, state: AnalyticsUiState) {
    // Summary Ring Chart
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Efficiency Rate", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            RingChart(
                trackedDuration = data.trackedDuration.inWholeMilliseconds,
                uniqueDuration = data.uniqueClockDuration.inWholeMilliseconds,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MetricItem("Unique", formatDuration(data.uniqueClockDuration), MaterialTheme.colorScheme.primary)
                MetricItem("Overlap", formatDuration(data.overlapDuration), MaterialTheme.colorScheme.error)
            }
        }
    }

    // Total Tracked Time Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Tracked", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(formatDuration(data.trackedDuration), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }

    // Concurrency Distribution
    Column {
        Text(
            "Concurrency Distribution", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (data.concurrencyDistribution.isEmpty()) {
            Text(
                "Chưa có dữ liệu phân bổ (No data)", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.concurrencyDistribution.forEach { (level, duration) ->
                        val fraction = if (data.uniqueClockDuration.inWholeMilliseconds > 0) {
                            duration.inWholeMilliseconds.toFloat() / data.uniqueClockDuration.inWholeMilliseconds
                        } else 0f
                        
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Level $level", fontWeight = FontWeight.Medium)
                                Text(formatDuration(duration), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = if(level > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Activity Breakdown
    Column {
        Text(
            "Activity Breakdown", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (data.activityDurations.isEmpty()) {
            Text(
                "Chưa có dữ liệu (No data)", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.activityDurations.entries.sortedByDescending { it.value }.forEach { (activityId, duration) ->
                        val activity = state.activities.find { it.id == activityId }
                        val color = activity?.color?.let { androidx.compose.ui.graphics.Color(it.toInt()) } ?: MaterialTheme.colorScheme.primary
                        
                        val fraction = if (data.trackedDuration.inWholeMilliseconds > 0) {
                            duration.inWholeMilliseconds.toFloat() / data.trackedDuration.inWholeMilliseconds
                        } else 0f
                        
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(activity?.name ?: "Unknown", fontWeight = FontWeight.Medium)
                                }
                                Text(formatDuration(duration), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = color,
                                trackColor = color.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    
    // Relationships
    RelationshipStats(
        relationships = data.activityRelationships,
        activities = state.activities,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun WeeklyAnalyticsContent(data: WeeklyAnalytics, state: AnalyticsUiState) {
    // Total Tracked Time Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Weekly Tracked", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(formatDuration(data.totalTrackedDuration), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }

    // Weekly Bar Chart
    Column {
        Text(
            "Weekly Trend", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            WeeklyBarChart(
                dailyBreakdown = data.dailyBreakdown,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // Activity Breakdown
    Column {
        Text(
            "Activity Breakdown", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (data.activityDurations.isEmpty()) {
            Text(
                "Chưa có dữ liệu (No data)", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.activityDurations.entries.sortedByDescending { it.value }.forEach { (activityId, duration) ->
                        val activity = state.activities.find { it.id == activityId }
                        val color = activity?.color?.let { androidx.compose.ui.graphics.Color(it.toInt()) } ?: MaterialTheme.colorScheme.primary
                        
                        val fraction = if (data.totalTrackedDuration.inWholeMilliseconds > 0) {
                            duration.inWholeMilliseconds.toFloat() / data.totalTrackedDuration.inWholeMilliseconds
                        } else 0f
                        
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(activity?.name ?: "Unknown", fontWeight = FontWeight.Medium)
                                }
                                Text(formatDuration(duration), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = color,
                                trackColor = color.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    
    // Relationships
    RelationshipStats(
        relationships = data.activityRelationships,
        activities = state.activities,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun MonthlyAnalyticsContent(data: MonthlyAnalytics, state: AnalyticsUiState) {
    // Total Tracked Time Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Monthly Tracked", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(formatDuration(data.totalTrackedDuration), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }

    // Monthly Heatmap
    Column {
        Text(
            "Monthly Heatmap", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            MonthlyHeatmap(
                yearMonth = data.yearMonth,
                dailyBreakdown = data.dailyBreakdown,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // Activity Breakdown
    Column {
        Text(
            "Activity Breakdown", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (data.activityDurations.isEmpty()) {
            Text(
                "Chưa có dữ liệu (No data)", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.activityDurations.entries.sortedByDescending { it.value }.forEach { (activityId, duration) ->
                        val activity = state.activities.find { it.id == activityId }
                        val color = activity?.color?.let { androidx.compose.ui.graphics.Color(it.toInt()) } ?: MaterialTheme.colorScheme.primary
                        
                        val fraction = if (data.totalTrackedDuration.inWholeMilliseconds > 0) {
                            duration.inWholeMilliseconds.toFloat() / data.totalTrackedDuration.inWholeMilliseconds
                        } else 0f
                        
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(activity?.name ?: "Unknown", fontWeight = FontWeight.Medium)
                                }
                                Text(formatDuration(duration), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = color,
                                trackColor = color.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Relationships
    RelationshipStats(
        relationships = data.activityRelationships,
        activities = state.activities,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun MetricItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun formatDuration(duration: Duration): String {
    duration.toComponents { hours, minutes, seconds, _ ->
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
