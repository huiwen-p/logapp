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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val sessionRepository: SessionRepository,
    private val startActivityUseCase: StartActivityUseCase,
    private val stopActivityUseCase: StopActivityUseCase
) : ViewModel() {

    // Only show unarchived activities for quick tracking
    val unarchivedActivities: StateFlow<List<ActivityEntity>> = activityRepository.getAllActivities()
        .map { activities -> activities.filter { !it.isArchived }.sortedBy { it.sortOrder } }
        .stateIn(
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

    fun addSampleActivities() {
        viewModelScope.launch {
            val sampleActivities = listOf(
                ActivityEntity(name = "Lập trình", color = 0xFF2196F3, createdAt = java.time.Instant.now(), updatedAt = java.time.Instant.now()),
                ActivityEntity(name = "Đọc sách", color = 0xFF4CAF50, createdAt = java.time.Instant.now(), updatedAt = java.time.Instant.now()),
                ActivityEntity(name = "Thể dục", color = 0xFFFF9800, createdAt = java.time.Instant.now(), updatedAt = java.time.Instant.now()),
                ActivityEntity(name = "Nghỉ ngơi", color = 0xFF9E9E9E, createdAt = java.time.Instant.now(), updatedAt = java.time.Instant.now())
            )
            sampleActivities.forEach { activityRepository.addActivity(it) }
        }
    }
}
