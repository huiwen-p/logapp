package com.example.logapp.feature.management

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.feature.management.components.ActivityFormBottomSheet
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageActivitiesScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageActivitiesViewModel = hiltViewModel()
) {
    val activities by viewModel.activities.collectAsState()
    
    // Filter active vs archived
    var selectedTabIndex by remember { mutableStateOf(0) }
    val displayList = remember(activities, selectedTabIndex) {
        if (selectedTabIndex == 0) {
            activities.filter { !it.isArchived }
        } else {
            activities.filter { it.isArchived }
        }
    }

    // Local state for dragging
    var reorderableList by remember { mutableStateOf(displayList) }
    
    // Sync local state when source changes
    LaunchedEffect(displayList) {
        reorderableList = displayList
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<ActivityEntity?>(null) }

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            reorderableList = reorderableList.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { startIndex, endIndex ->
            if (startIndex != endIndex) {
                viewModel.updateSortOrder(reorderableList)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Hoạt động") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(onClick = { 
                    activityToEdit = null
                    showBottomSheet = true 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Activity")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                    Text("Đang theo dõi", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                    Text("Đã lưu trữ", modifier = Modifier.padding(16.dp))
                }
            }

            if (reorderableList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có dữ liệu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = state.listState,
                    modifier = Modifier.reorderable(state).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reorderableList, { it.id }) { item ->
                        ReorderableItem(state, key = item.id) { isDragging ->
                            val elevation = if (isDragging) 8.dp else 0.dp
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(elevation, shape = MaterialTheme.shapes.medium),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                ),
                                onClick = { 
                                    activityToEdit = item
                                    showBottomSheet = true 
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(item.color?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (selectedTabIndex == 0) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Drag to reorder",
                                            modifier = Modifier.detectReorderAfterLongPress(state),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ActivityFormBottomSheet(
            activity = activityToEdit,
            onDismiss = { showBottomSheet = false },
            onSave = { name, color, icon ->
                viewModel.saveActivity(activityToEdit?.id, name, color, icon)
                showBottomSheet = false
            },
            onArchive = { id ->
                viewModel.archiveActivity(id, archive = true)
                showBottomSheet = false
            },
            onUnarchive = { id ->
                viewModel.archiveActivity(id, archive = false)
                showBottomSheet = false
            }
        )
    }
}
