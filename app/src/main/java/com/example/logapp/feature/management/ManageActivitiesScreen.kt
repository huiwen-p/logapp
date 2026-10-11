package com.example.logapp.feature.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logapp.core.designsystem.component.*
import com.example.logapp.core.designsystem.tokens.spacing
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.feature.management.components.ActivityFormBottomSheet
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageActivitiesScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageActivitiesViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeActivities by viewModel.activeActivities.collectAsState()
    val archivedActivities by viewModel.archivedActivities.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val archivedCount by viewModel.archivedCount.collectAsState()
    val operationEvent by viewModel.operationEvent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationEvent) {
        operationEvent?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearOperationEvent()
        }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<ActivityEntity?>(null) }

    // Dialog confirmation states
    var activityToArchive by remember { mutableStateOf<ActivityEntity?>(null) }
    var activityToUnarchive by remember { mutableStateOf<ActivityEntity?>(null) }
    var activityToDelete by remember { mutableStateOf<ActivityEntity?>(null) }
    var activityBlockedFromDelete by remember { mutableStateOf<Pair<ActivityEntity, Int>?>(null) }

    // Local state for dragging active activities
    var reorderableList by remember { mutableStateOf(activeActivities) }
    LaunchedEffect(activeActivities) {
        reorderableList = activeActivities
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quản lý Hoạt động",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = {
                        activityToEdit = null
                        showBottomSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm hoạt động mới")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Quick Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Tìm kiếm theo tên hoạt động...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Xóa tìm kiếm",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium, vertical = spacing.small)
            )

            // Tabs Segment
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đang theo dõi",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        LogAppStatusBadge(
                            status = if (selectedTabIndex == 0) StatusType.INFO else StatusType.NEUTRAL,
                            text = activeCount.toString()
                        )
                    }
                }
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đã lưu trữ",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        LogAppStatusBadge(
                            status = if (selectedTabIndex == 1) StatusType.WARNING else StatusType.NEUTRAL,
                            text = archivedCount.toString()
                        )
                    }
                }
            }

            if (errorMessage != null) {
                LogAppErrorState(
                    title = "Không thể tải danh sách hoạt động",
                    message = errorMessage ?: "Đã xảy ra lỗi không xác định.",
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small)
                )
            }

            // Tab 0: Active Activities (Supports Reordering when search is empty)
            if (selectedTabIndex == 0) {
                if (isLoading && reorderableList.isEmpty()) {
                    SkeletonActivityList(
                        itemCount = 5,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.medium)
                    )
                } else if (reorderableList.isEmpty()) {
                    if (searchQuery.isNotBlank()) {
                        EmptySearchResultCard(
                            query = searchQuery,
                            onClearSearch = { viewModel.onSearchQueryChange("") }
                        )
                    } else {
                        EmptyActiveActivitiesCard(
                            onCreateNew = {
                                activityToEdit = null
                                showBottomSheet = true
                            }
                        )
                    }
                } else {
                    // Reorder Hint Banner
                    if (searchQuery.isBlank() && reorderableList.size > 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.medium, vertical = spacing.extraSmall)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(spacing.small))
                                Text(
                                    text = "Nhấn giữ biểu tượng ☰ để kéo thả sắp xếp thứ tự hiển thị",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (searchQuery.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.medium, vertical = spacing.extraSmall)
                        ) {
                            Text(
                                text = "Đang tìm kiếm theo \"$searchQuery\" — Xóa bộ lọc để kéo thả sắp xếp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small)
                            )
                        }
                    }

                    LazyColumn(
                        state = state.listState,
                        modifier = Modifier
                            .reorderable(state)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = spacing.medium,
                            end = spacing.medium,
                            top = spacing.small,
                            bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        items(reorderableList, { it.id }) { item ->
                            ReorderableItem(state, key = item.id) { isDragging ->
                                val elevation = if (isDragging) 8.dp else 1.dp
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(elevation, shape = RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDragging) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                    onClick = {
                                        activityToEdit = item
                                        showBottomSheet = true
                                    }
                                ) {
                                    ActivityItemRow(
                                        activity = item,
                                        isArchivedTab = false,
                                        isSearchActive = searchQuery.isNotBlank(),
                                        reorderModifier = Modifier.detectReorderAfterLongPress(state),
                                        onEdit = {
                                            activityToEdit = item
                                            showBottomSheet = true
                                        },
                                        onArchive = { activityToArchive = item }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Archived Activities
                if (isLoading && archivedActivities.isEmpty()) {
                    SkeletonActivityList(
                        itemCount = 3,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.medium)
                    )
                } else if (archivedActivities.isEmpty()) {
                    if (searchQuery.isNotBlank()) {
                        EmptySearchResultCard(
                            query = searchQuery,
                            onClearSearch = { viewModel.onSearchQueryChange("") }
                        )
                    } else {
                        EmptyArchivedActivitiesCard()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = spacing.medium,
                            end = spacing.medium,
                            top = spacing.small,
                            bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        items(archivedActivities, { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ),
                                onClick = {
                                    activityToEdit = item
                                    showBottomSheet = true
                                }
                            ) {
                                ActivityItemRow(
                                    activity = item,
                                    isArchivedTab = true,
                                    isSearchActive = searchQuery.isNotBlank(),
                                    reorderModifier = Modifier,
                                    onEdit = {
                                        activityToEdit = item
                                        showBottomSheet = true
                                    },
                                    onUnarchive = { activityToUnarchive = item },
                                    onDelete = {
                                        coroutineScope.launch {
                                            val count = viewModel.getSessionCount(item.id)
                                            if (count > 0) {
                                                activityBlockedFromDelete = Pair(item, count)
                                            } else {
                                                activityToDelete = item
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Create / Edit
    if (showBottomSheet) {
        ActivityFormBottomSheet(
            activity = activityToEdit,
            onDismiss = { showBottomSheet = false },
            onSave = { name, color, icon ->
                val saved = viewModel.saveActivity(activityToEdit?.id, name, color, icon)
                if (saved) showBottomSheet = false
            },
            onArchive = { id ->
                val item = (activeActivities + archivedActivities).find { it.id == id }
                showBottomSheet = false
                activityToArchive = item
            },
            onUnarchive = { id ->
                val item = (activeActivities + archivedActivities).find { it.id == id }
                showBottomSheet = false
                activityToUnarchive = item
            },
            onDelete = { id ->
                val item = (activeActivities + archivedActivities).find { it.id == id }
                showBottomSheet = false
                if (item != null) {
                    coroutineScope.launch {
                        val count = viewModel.getSessionCount(item.id)
                        if (count > 0) {
                            activityBlockedFromDelete = Pair(item, count)
                        } else {
                            activityToDelete = item
                        }
                    }
                }
            },
            onValidateName = { id, name ->
                viewModel.validateActivityName(id, name)
            }
        )
    }

    // Confirm Archive Dialog
    activityToArchive?.let { activity ->
        LogAppConfirmDialog(
            title = "Lưu trữ hoạt động?",
            message = "Hoạt động \"${activity.name}\" sẽ được ẩn khỏi màn hình theo dõi nhanh. Toàn bộ lịch sử trong Nhật ký và Thống kê vẫn được bảo toàn nguyên vẹn.",
            confirmText = "Lưu trữ",
            dismissText = "Hủy",
            icon = Icons.Default.Info,
            onConfirm = {
                viewModel.archiveActivity(activity.id, archive = true)
                activityToArchive = null
            },
            onDismiss = { activityToArchive = null }
        )
    }

    // Confirm Unarchive Dialog
    activityToUnarchive?.let { activity ->
        LogAppConfirmDialog(
            title = "Khôi phục hoạt động?",
            message = "Hoạt động \"${activity.name}\" sẽ xuất hiện trở lại trên màn hình theo dõi nhanh và danh sách hoạt động đang theo dõi.",
            confirmText = "Khôi phục",
            dismissText = "Hủy",
            icon = Icons.Default.Info,
            onConfirm = {
                viewModel.archiveActivity(activity.id, archive = false)
                activityToUnarchive = null
            },
            onDismiss = { activityToUnarchive = null }
        )
    }

    // Confirm Hard Delete Dialog (When session count == 0)
    activityToDelete?.let { activity ->
        LogAppConfirmDialog(
            title = "Xóa vĩnh viễn hoạt động?",
            message = "Hoạt động \"${activity.name}\" chưa có bất kỳ phiên ghi nhận nào trong lịch sử. Bạn có chắc muốn xóa vĩnh viễn không? Hành động này không thể hoàn tác.",
            confirmText = "Xóa vĩnh viễn",
            dismissText = "Hủy",
            isDestructive = true,
            icon = Icons.Default.Warning,
            onConfirm = {
                viewModel.deleteActivity(
                    id = activity.id,
                    onHasSessions = { count, act ->
                        activityBlockedFromDelete = Pair(act, count)
                    },
                    onSuccess = { activityToDelete = null }
                )
                activityToDelete = null
            },
            onDismiss = { activityToDelete = null }
        )
    }

    // Blocked from Delete Dialog (When session count > 0)
    activityBlockedFromDelete?.let { (activity, count) ->
        LogAppConfirmDialog(
            title = "Không thể xóa vĩnh viễn",
            message = "Hoạt động \"${activity.name}\" đã có $count phiên ghi nhận trong quá khứ. Để bảo vệ dữ liệu lịch sử và báo cáo, ứng dụng khuyến nghị bạn Lưu trữ (Archive) thay vì xóa.",
            confirmText = "Lưu trữ hoạt động",
            dismissText = "Đóng",
            icon = Icons.Default.Warning,
            onConfirm = {
                viewModel.archiveActivity(activity.id, archive = true)
                activityBlockedFromDelete = null
            },
            onDismiss = { activityBlockedFromDelete = null }
        )
    }
}

@Composable
private fun ActivityItemRow(
    activity: ActivityEntity,
    isArchivedTab: Boolean,
    isSearchActive: Boolean,
    reorderModifier: Modifier,
    onEdit: () -> Unit,
    onArchive: (() -> Unit)? = null,
    onUnarchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val activityColor = activity.color?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium, vertical = spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon / Emoji Badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(activityColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (!activity.icon.isNullOrBlank()) {
                Text(text = activity.icon, fontSize = 20.sp)
            } else {
                Text(
                    text = activity.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = activityColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(spacing.medium))

        // Activity Name and Status
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isArchivedTab) {
                Text(
                    text = "Đã lưu trữ (Ẩn khỏi trang chủ)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Thứ tự ưu tiên #${activity.sortOrder + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Buttons
        if (isArchivedTab) {
            IconButton(onClick = { onUnarchive?.invoke() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Khôi phục",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { onDelete?.invoke() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Chỉnh sửa",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Drag handle only when not searching
            if (!isSearchActive) {
                IconButton(
                    onClick = {},
                    modifier = reorderModifier
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Kéo để sắp xếp",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySearchResultCard(
    query: String,
    onClearSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        LogAppEmptyState(
            icon = Icons.Default.Search,
            title = "Không tìm thấy hoạt động \"$query\"",
            message = "Kiểm tra lại từ khóa tìm kiếm hoặc bấm nút bên dưới để xem toàn bộ danh sách.",
            secondaryButtonText = "Xem tất cả hoạt động",
            onSecondaryClick = onClearSearch
        )
    }
}

@Composable
private fun EmptyActiveActivitiesCard(
    onCreateNew: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        LogAppEmptyState(
            icon = Icons.Default.Add,
            title = "Chưa có hoạt động đang theo dõi",
            message = "Tạo các hoạt động quen thuộc của bạn (ví dụ: Lập trình, Học tập, Tập gym) để theo dõi thời gian dễ dàng chỉ với một chạm.",
            primaryButtonText = "Tạo hoạt động mới",
            primaryButtonIcon = Icons.Default.Add,
            onPrimaryClick = onCreateNew
        )
    }
}

@Composable
private fun EmptyArchivedActivitiesCard() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        LogAppEmptyState(
            icon = Icons.Default.Info,
            title = "Chưa có hoạt động nào được lưu trữ",
            message = "Khi một hoạt động không còn thường xuyên sử dụng, bạn có thể Lưu trữ (Archive) nó để ẩn khỏi trang chủ mà vẫn giữ nguyên 100% lịch sử trong Nhật ký và Thống kê."
        )
    }
}
