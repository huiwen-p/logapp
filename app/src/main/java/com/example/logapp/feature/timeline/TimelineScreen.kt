package com.example.logapp.feature.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logapp.data.local.entity.ActivitySessionEntity
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedSession by remember { mutableStateOf<ActivitySessionEntity?>(null) }

    var scaleY by remember { mutableStateOf(1f) }
    var offsetY by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Timeline", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, style = MaterialTheme.typography.displayMedium) }
            ) 
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            com.example.logapp.feature.timeline.components.TimelineCanvas(
                sessions = state.sessions,
                activities = state.activities,
                onSessionClick = { session -> selectedSession = session }
            )
        }

        selectedSession?.let { session ->
            val activity = state.activities.find { it.id == session.activityId }
            SessionBottomSheet(
                session = session,
                activity = activity,
                onDismiss = { selectedSession = null },
                onSave = { newStart, newEnd, newNote ->
                    viewModel.editSession(session, newStart, newEnd, newNote)
                    selectedSession = null
                },
                onDelete = {
                    viewModel.deleteSession(session)
                    selectedSession = null
                }
            )
        }
    }
}
