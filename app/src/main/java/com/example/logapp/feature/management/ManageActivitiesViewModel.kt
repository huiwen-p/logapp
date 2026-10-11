package com.example.logapp.feature.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.usecase.DeleteActivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ManageActivitiesViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val sessionRepository: SessionRepository,
    private val deleteActivityUseCase: DeleteActivityUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _operationEvent = MutableStateFlow<String?>(null)
    val operationEvent: StateFlow<String?> = _operationEvent.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                activityRepository.getAllActivities().collect {
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Không thể tải danh sách hoạt động: ${e.localizedMessage ?: "Lỗi hệ thống"}"
            }
        }
    }

    fun retry() {
        loadData()
    }

    val rawActivities: StateFlow<List<ActivityEntity>> = activityRepository.getAllActivities()
        .map { list -> list.sortedBy { it.sortOrder } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val activities: StateFlow<List<ActivityEntity>> = rawActivities

    val activeActivities: StateFlow<List<ActivityEntity>> = combine(rawActivities, _searchQuery) { list, query ->
        val activeList = list.filter { !it.isArchived }
        if (query.isBlank()) activeList
        else activeList.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val archivedActivities: StateFlow<List<ActivityEntity>> = combine(rawActivities, _searchQuery) { list, query ->
        val archivedList = list.filter { it.isArchived }
        if (query.isBlank()) archivedList
        else archivedList.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val activeCount: StateFlow<Int> = rawActivities
        .map { list -> list.count { !it.isArchived } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val archivedCount: StateFlow<Int> = rawActivities
        .map { list -> list.count { it.isArchived } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearOperationEvent() {
        _operationEvent.value = null
    }

    fun validateActivityName(id: String?, name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return "Tên hoạt động không được để trống"
        }
        val isDuplicate = rawActivities.value.any {
            it.id != id && it.name.trim().equals(trimmed, ignoreCase = true)
        }
        if (isDuplicate) {
            return "Tên hoạt động \"$trimmed\" đã tồn tại"
        }
        return null
    }

    fun saveActivity(id: String?, name: String, color: Long, iconName: String?): Boolean {
        val validationError = validateActivityName(id, name)
        if (validationError != null) {
            _operationEvent.value = validationError
            return false
        }

        viewModelScope.launch {
            val trimmed = name.trim()
            val finalIcon = iconName?.takeIf { it.isNotBlank() }
            if (id == null) {
                val currentMax = rawActivities.value.maxOfOrNull { it.sortOrder } ?: -1
                val newActivity = ActivityEntity(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    color = color,
                    icon = finalIcon,
                    sortOrder = currentMax + 1,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                activityRepository.addActivity(newActivity)
                _operationEvent.value = "Đã tạo hoạt động \"$trimmed\""
            } else {
                val existing = rawActivities.value.find { it.id == id }
                if (existing != null) {
                    val updated = existing.copy(
                        name = trimmed,
                        color = color,
                        icon = finalIcon,
                        updatedAt = Instant.now()
                    )
                    activityRepository.updateActivity(updated)
                    _operationEvent.value = "Đã cập nhật hoạt động \"$trimmed\""
                }
            }
        }
        return true
    }

    fun archiveActivity(id: String, archive: Boolean = true) {
        viewModelScope.launch {
            val existing = rawActivities.value.find { it.id == id }
            if (existing != null) {
                activityRepository.updateActivity(
                    existing.copy(
                        isArchived = archive,
                        updatedAt = Instant.now()
                    )
                )
                _operationEvent.value = if (archive) {
                    "Đã lưu trữ hoạt động \"${existing.name}\""
                } else {
                    "Đã khôi phục hoạt động \"${existing.name}\""
                }
            }
        }
    }

    suspend fun getSessionCount(activityId: String): Int {
        return sessionRepository.getSessionsForActivity(activityId).firstOrNull()?.size ?: 0
    }

    fun deleteActivity(
        id: String,
        onHasSessions: (sessionCount: Int, activity: ActivityEntity) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = rawActivities.value.find { it.id == id } ?: return@launch
            val sessionCount = getSessionCount(id)
            if (sessionCount > 0) {
                onHasSessions(sessionCount, existing)
            } else {
                val result = deleteActivityUseCase(existing)
                if (result.isSuccess) {
                    _operationEvent.value = "Đã xóa vĩnh viễn hoạt động \"${existing.name}\""
                    onSuccess()
                } else {
                    _operationEvent.value = result.exceptionOrNull()?.message ?: "Lỗi khi xóa hoạt động"
                }
            }
        }
    }

    fun updateSortOrder(newOrderedList: List<ActivityEntity>) {
        viewModelScope.launch {
            newOrderedList.forEachIndexed { index, activity ->
                if (activity.sortOrder != index) {
                    activityRepository.updateActivity(
                        activity.copy(
                            sortOrder = index,
                            updatedAt = Instant.now()
                        )
                    )
                }
            }
        }
    }
}
