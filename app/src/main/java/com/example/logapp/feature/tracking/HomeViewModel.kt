package com.example.logapp.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.usecase.StartActivityUseCase
import com.example.logapp.domain.usecase.StopActivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val sessionRepository: SessionRepository,
    private val startActivityUseCase: StartActivityUseCase,
    private val stopActivityUseCase: StopActivityUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Only show unarchived activities for quick tracking
    val unarchivedActivities: StateFlow<List<ActivityEntity>> = activityRepository.getAllActivities()
        .map { activities -> activities.filter { !it.isArchived }.sortedBy { it.sortOrder } }
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

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun startActivity(activityId: String) {
        viewModelScope.launch {
            startActivityUseCase(activityId)
        }
    }

    fun stopSession(session: ActivitySessionEntity) {
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
