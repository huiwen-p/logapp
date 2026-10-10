package com.example.logapp.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logapp.data.local.entity.ActivitySessionEntity
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedSession by remember { mutableStateOf<ActivitySessionEntity?>(null) }
    var showCreateSession by remember { mutableStateOf(false) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedSessionIds by remember { mutableStateOf(setOf<String>()) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Scaffold(
        topBar = { 
            if (isMultiSelectMode) {
                TopAppBar(
                    title = { Text("${selectedSessionIds.size} đã chọn") },
                    navigationIcon = {
                        IconButton(onClick = { 
                            isMultiSelectMode = false 
                            selectedSessionIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Hủy")
                        }
                    },
                    actions = {
                        if (selectedSessionIds.size == 2) {
                            TextButton(onClick = {
                                val s1 = state.sessions.find { it.id == selectedSessionIds.toList()[0] }
                                val s2 = state.sessions.find { it.id == selectedSessionIds.toList()[1] }
                                if (s1 != null && s2 != null) {
                                    viewModel.mergeSessions(s1, s2)
                                }
                                isMultiSelectMode = false
                                selectedSessionIds = emptySet()
                            }) {
                                Text("Gộp", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Nhật ký", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displayMedium) }
                ) 
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateSession = true }) {
                Icon(Icons.Default.Add, contentDescription = "Thêm thủ công")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Date Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changeDate(-1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Day")
                }
                Text(
                    text = state.selectedDate.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { viewModel.changeDate(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Day")
                }
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            com.example.logapp.feature.timeline.components.TimelineCanvas(
                selectedDate = state.selectedDate,
                sessions = state.sessions,
                activities = state.activities,
                onSessionClick = { session ->
                    if (isMultiSelectMode) {
                        if (selectedSessionIds.contains(session.id)) {
                            selectedSessionIds = selectedSessionIds - session.id
                            if (selectedSessionIds.isEmpty()) isMultiSelectMode = false
                        } else {
                            if (selectedSessionIds.size < 2) {
                                selectedSessionIds = selectedSessionIds + session.id
                            }
                        }
                    } else {
                        selectedSession = session
                    }
                },
                onSessionLongClick = { session ->
                    isMultiSelectMode = true
                    selectedSessionIds = selectedSessionIds + session.id
                },
                selectedSessionIds = selectedSessionIds,
                modifier = Modifier.weight(1f)
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
                },
                onSplit = { splitTime ->
                    viewModel.splitSession(session, splitTime)
                    selectedSession = null
                }
            )
        }

        if (showCreateSession) {
            CreateSessionBottomSheet(
                activities = state.activities,
                onDismiss = { showCreateSession = false },
                onSave = { activityId, start, end, note ->
                    viewModel.createManualSession(activityId, start, end, note)
                    showCreateSession = false
                }
            )
        }
    }
}
