package com.example.logapp.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.usecase.StartActivityUseCase
import com.example.logapp.domain.usecase.StopActivityUseCase
import com.example.logapp.core.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val sessionRepository: SessionRepository,
    private val startActivityUseCase: StartActivityUseCase,
    private val stopActivityUseCase: StopActivityUseCase,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor?.isOnline
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        ?: MutableStateFlow(true).asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            activityRepository.getAllActivities()
                .catch { e ->
                    _isLoading.value = false
                    _errorMessage.value = "Không thể tải dữ liệu: ${e.localizedMessage ?: "Lỗi hệ thống"}"
                }
                .collect {
                    _isLoading.value = false
                }
        }
    }

    fun retry() {
        loadData()
    }

    // Only show unarchived activities for quick tracking
    val unarchivedActivities: StateFlow<List<ActivityEntity>> = activityRepository.getAllActivities()
        .map { activities -> activities.filter { !it.isArchived }.sortedBy { it.sortOrder } }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredActivities: StateFlow<List<ActivityEntity>> = combine(
        unarchivedActivities,
        _searchQuery
    ) { activities, query ->
        if (query.isBlank()) {
            activities
        } else {
            activities.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeSessions: StateFlow<List<ActivitySessionEntity>> = sessionRepository.getActiveSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todaySessionsCount: StateFlow<Int> = sessionRepository.getAllSessions()
        .map { sessions ->
            val startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            sessions.count { it.startedAt.isAfter(startOfToday) }
        }
        .catch { emit(0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private var lastActionTimestamp = 0L

    fun toggleActivity(activityId: String) {
        val now = System.currentTimeMillis()
        if (now - lastActionTimestamp < 300L) return
        lastActionTimestamp = now

        viewModelScope.launch {
            val runningSession = activeSessions.value.find { it.activityId == activityId }
            if (runningSession != null) {
                stopActivityUseCase(runningSession)
            } else {
                startActivityUseCase(activityId)
            }
        }
    }

    fun startActivity(activityId: String) {
        val now = System.currentTimeMillis()
        if (now - lastActionTimestamp < 300L) return
        lastActionTimestamp = now

        viewModelScope.launch {
            val alreadyRunning = activeSessions.value.any { it.activityId == activityId }
            if (!alreadyRunning) {
                startActivityUseCase(activityId)
            }
        }
    }

    fun stopSession(session: ActivitySessionEntity) {
        val now = System.currentTimeMillis()
        if (now - lastActionTimestamp < 250L) return
        lastActionTimestamp = now

        viewModelScope.launch {
            stopActivityUseCase(session)
        }
    }

    fun undoStopSession(session: ActivitySessionEntity) {
        viewModelScope.launch {
            val restored = session.copy(endedAt = null, updatedAt = Instant.now())
            sessionRepository.updateSession(restored)
        }
    }

    fun addSampleActivities() {
        viewModelScope.launch {
            val sampleActivities = listOf(
                ActivityEntity(name = "Lập trình", color = 0xFF2196F3, createdAt = Instant.now(), updatedAt = Instant.now()),
                ActivityEntity(name = "Đọc sách", color = 0xFF4CAF50, createdAt = Instant.now(), updatedAt = Instant.now()),
                ActivityEntity(name = "Thể dục", color = 0xFFFF9800, createdAt = Instant.now(), updatedAt = Instant.now()),
                ActivityEntity(name = "Nghỉ ngơi", color = 0xFF9E9E9E, createdAt = Instant.now(), updatedAt = Instant.now())
            )
            sampleActivities.forEach { activityRepository.addActivity(it) }
        }
    }
}
