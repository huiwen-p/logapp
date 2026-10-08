package com.example.logapp.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.engine.AnalyticsEngine
import com.example.logapp.domain.engine.DailyAnalytics
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.usecase.DeleteSessionUseCase
import com.example.logapp.domain.usecase.EditSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class TimelineUiState(
    val sessions: List<ActivitySessionEntity> = emptyList(),
    val activities: List<ActivityEntity> = emptyList(),
    val dailyAnalytics: DailyAnalytics? = null
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val activityRepository: ActivityRepository,
    private val editSessionUseCase: EditSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase
) : ViewModel() {

    val uiState: StateFlow<TimelineUiState> = combine(
        sessionRepository.getAllSessions(),
        activityRepository.getAllActivities()
    ) { sessions, activities ->
        val analytics = AnalyticsEngine.calculateDailyAnalytics(sessions, Instant.now())
        TimelineUiState(sessions, activities, analytics)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState())

    fun editSession(session: ActivitySessionEntity, newStartedAt: Instant, newEndedAt: Instant?, newNote: String?) {
        viewModelScope.launch {
            editSessionUseCase(session, newStartedAt, newEndedAt, newNote)
        }
    }

    fun deleteSession(session: ActivitySessionEntity) {
        viewModelScope.launch {
            deleteSessionUseCase(session)
        }
    }
}
