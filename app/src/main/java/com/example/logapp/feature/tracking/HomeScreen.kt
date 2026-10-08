package com.example.logapp.feature.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.feature.components.ActivityCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToActivities: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val activities by viewModel.unarchivedActivities.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text("TimeLens", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displayMedium) 
                },
                actions = {
                    TextButton(onClick = onNavigateToActivities) {
                        Text("Manage", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Active Sessions Block
            AnimatedVisibility(
                visible = activeSessions.isNotEmpty(),
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(
                        text = "ĐANG THEO DÕI",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        activeSessions.forEach { session ->
                            val activity = activities.find { it.id == session.activityId }
                            ActiveSessionItem(
                                session = session,
                                activityName = activity?.name ?: "Unknown",
                                onStop = { 
                                    viewModel.stopSession(session)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Đã dừng ${activity?.name}",
                                            actionLabel = "Hoàn tác",
                                            duration = SnackbarDuration.Short
                                        )
                                        // Note: Real Undo logic to be wired in SessionRepository in future
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = "QUICK TRACKING",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp, top = if(activeSessions.isEmpty()) 8.dp else 0.dp)
            )
            
            if (activities.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Chưa có hoạt động nào", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { viewModel.addSampleActivities() }) {
                            Text("Thêm dữ liệu mẫu (Sample Data)")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(activities, key = { it.id }) { activity ->
                        ActivityCard(
                            name = activity.name,
                            color = activity.color,
                            onClick = { viewModel.startActivity(activity.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveSessionItem(
    session: ActivitySessionEntity,
    activityName: String,
    onStop: () -> Unit
) {
    var duration by remember { mutableStateOf(Duration.between(session.startedAt, Instant.now())) }

    LaunchedEffect(session.startedAt) {
        while (true) {
            duration = Duration.between(session.startedAt, Instant.now())
            delay(1000)
        }
    }

    val hours = duration.toHours()
    val minutes = duration.toMinutesPart()
    val seconds = duration.toSecondsPart()
    val durationString = "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulse indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935)) // Red indicator for active
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activityName, 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = durationString, 
                    style = MaterialTheme.typography.titleLarge, 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }
            
            FilledTonalButton(
                onClick = onStop,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Dừng", fontWeight = FontWeight.Bold)
            }
        }
    }
}
