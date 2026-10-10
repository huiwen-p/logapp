package com.example.logapp.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.domain.engine.AnalyticsEngine
import com.example.logapp.domain.engine.DailyAnalytics
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.domain.usecase.CreateManualSessionUseCase
import com.example.logapp.domain.usecase.DeleteSessionUseCase
import com.example.logapp.domain.usecase.EditSessionUseCase
import com.example.logapp.domain.usecase.SplitSessionUseCase
import com.example.logapp.domain.usecase.MergeSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import java.time.LocalDate
import java.time.ZoneId
import com.example.logapp.data.datastore.PreferencesManager

data class TimelineUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val sessions: List<ActivitySessionEntity> = emptyList(),
    val activities: List<ActivityEntity> = emptyList(),
    val dailyAnalytics: DailyAnalytics? = null
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val activityRepository: ActivityRepository,
    private val createManualSessionUseCase: CreateManualSessionUseCase,
    private val editSessionUseCase: EditSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val splitSessionUseCase: SplitSessionUseCase,
    private val mergeSessionUseCase: MergeSessionUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<TimelineUiState> = combine(
        sessionRepository.getAllSessions(),
        activityRepository.getAllActivities(),
        _selectedDate,
        preferencesManager.dayBoundary
    ) { sessions, activities, date, boundaryStr ->
        val (hours, minutes) = boundaryStr.split(":").map { it.toIntOrNull() ?: 0 }
        
        val startOfDay = date.atTime(hours, minutes).atZone(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atTime(hours, minutes).atZone(ZoneId.systemDefault()).toInstant()
        
        val dailySessions = sessions.filter {
            val s = it.startedAt
            val e = it.endedAt ?: Instant.now()
            !(e.isBefore(startOfDay) || s.isAfter(endOfDay))
        }

        val analytics = AnalyticsEngine.calculateDailyAnalytics(dailySessions, Instant.now(), startOfDay, endOfDay)
        TimelineUiState(date, dailySessions, activities, analytics)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState())

    fun changeDate(offsetDays: Long) {
        _selectedDate.value = _selectedDate.value.plusDays(offsetDays)
    }

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

    fun createManualSession(activityId: String, startTime: Instant, endTime: Instant, note: String?) {
        viewModelScope.launch {
            try {
                createManualSessionUseCase(activityId, startTime, endTime, note)
            } catch (e: Exception) {
                // Ignore or handle UI error states if needed
            }
        }
    }

    fun splitSession(session: ActivitySessionEntity, splitAt: Instant) {
        viewModelScope.launch {
            try {
                splitSessionUseCase(session, splitAt)
            } catch (e: Exception) {
                // Ignore or handle UI error states if needed
            }
        }
    }

    fun mergeSessions(session1: ActivitySessionEntity, session2: ActivitySessionEntity) {
        viewModelScope.launch {
            try {
                mergeSessionUseCase(session1, session2)
            } catch (e: Exception) {
                // Ignore or handle UI error states if needed
            }
        }
    }
}
