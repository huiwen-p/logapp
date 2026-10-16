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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import androidx.compose.runtime.Immutable
import javax.inject.Inject
import java.time.LocalDate
import java.time.ZoneId
import com.example.logapp.data.datastore.PreferencesManager

enum class TimelineViewMode {
    CANVAS,
    LIST
}

@Immutable
data class TimelineUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val sessions: List<ActivitySessionEntity> = emptyList(),
    val activities: List<ActivityEntity> = emptyList(),
    val dailyAnalytics: DailyAnalytics? = null,
    val viewMode: TimelineViewMode = TimelineViewMode.CANVAS,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
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
    private val _viewMode = MutableStateFlow(TimelineViewMode.CANVAS)
    val viewMode: StateFlow<TimelineViewMode> = _viewMode.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == TimelineViewMode.CANVAS) TimelineViewMode.LIST else TimelineViewMode.CANVAS
    }

    fun setViewMode(mode: TimelineViewMode) {
        _viewMode.value = mode
    }

    val uiState: StateFlow<TimelineUiState> = combine(
        sessionRepository.getAllSessions(),
        activityRepository.getAllActivities(),
        _selectedDate,
        _viewMode
    ) { sessions, activities, date, mode ->
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant()
        
        val dailySessions = sessions.filter {
            val s = it.startedAt
            val e = it.endedAt ?: Instant.now()
            !(e.isBefore(startOfDay) || s.isAfter(endOfDay))
        }

        val analytics = AnalyticsEngine.calculateDailyAnalytics(dailySessions, Instant.now(), startOfDay, endOfDay)
        TimelineUiState(
            selectedDate = date,
            sessions = dailySessions,
            activities = activities,
            dailyAnalytics = analytics,
            viewMode = mode,
            isLoading = false,
            errorMessage = null
        )
    }
    .flowOn(Dispatchers.Default)
    .catch { e ->
        emit(TimelineUiState(selectedDate = _selectedDate.value, isLoading = false, errorMessage = "Lỗi tải dữ liệu: ${e.localizedMessage ?: "Lỗi hệ thống"}"))
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState(isLoading = true))

    fun changeDate(offsetDays: Long) {
        _selectedDate.value = _selectedDate.value.plusDays(offsetDays)
    }

    fun jumpToToday() {
        _selectedDate.value = LocalDate.now()
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
                _userMessage.value = "Không thể tạo phiên: ${e.localizedMessage ?: "Lỗi không xác định"}"
            }
        }
    }

    fun splitSession(session: ActivitySessionEntity, splitAt: Instant) {
        viewModelScope.launch {
            try {
                splitSessionUseCase(session, splitAt)
            } catch (e: Exception) {
                _userMessage.value = "Không thể chia phiên: ${e.localizedMessage ?: "Lỗi không xác định"}"
            }
        }
    }

    fun mergeSessions(session1: ActivitySessionEntity, session2: ActivitySessionEntity) {
        viewModelScope.launch {
            try {
                mergeSessionUseCase(session1, session2)
            } catch (e: Exception) {
                _userMessage.value = "Không thể gộp phiên: ${e.localizedMessage ?: "Lỗi không xác định"}"
            }
        }
    }
}
