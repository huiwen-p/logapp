package com.example.logapp.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logapp.domain.engine.AnalyticsEngine
import com.example.logapp.domain.engine.DailyAnalytics
import com.example.logapp.domain.engine.WeeklyAnalytics
import com.example.logapp.domain.engine.MonthlyAnalytics
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.domain.repository.ActivityRepository
import com.example.logapp.domain.repository.SessionRepository
import com.example.logapp.data.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class AnalyticsTab { DAILY, WEEKLY, MONTHLY }

data class AnalyticsUiState(
    val selectedTab: AnalyticsTab = AnalyticsTab.DAILY,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyAnalytics: DailyAnalytics? = null,
    val weeklyAnalytics: WeeklyAnalytics? = null,
    val monthlyAnalytics: MonthlyAnalytics? = null,
    val activities: List<ActivityEntity> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val activityRepository: ActivityRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(AnalyticsTab.DAILY)
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<AnalyticsUiState> = combine(
        sessionRepository.getAllSessions(),
        activityRepository.getAllActivities(),
        preferencesManager.dayBoundary,
        _selectedTab,
        _selectedDate
    ) { sessions, activities, boundaryStr, tab, date ->
        val (hours, minutes) = boundaryStr.split(":").map { it.toIntOrNull() ?: 0 }
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        
        val startOfDay = date.atTime(hours, minutes).atZone(zone).toInstant()
        val endOfDay = date.plusDays(1).atTime(hours, minutes).atZone(zone).toInstant()

        var dailyAnalytics: DailyAnalytics? = null
        var weeklyAnalytics: WeeklyAnalytics? = null
        var monthlyAnalytics: MonthlyAnalytics? = null

        if (tab == AnalyticsTab.DAILY) {
            dailyAnalytics = AnalyticsEngine.calculateDailyAnalytics(sessions, now, startOfDay, endOfDay)
        } else if (tab == AnalyticsTab.WEEKLY) {
            val weekStart = date.minusDays((date.dayOfWeek.value - 1).toLong()) // Monday
            val boundaries = (0..6).associate { i ->
                val day = weekStart.plusDays(i.toLong())
                val dStart = day.atTime(hours, minutes).atZone(zone).toInstant()
                val dEnd = day.plusDays(1).atTime(hours, minutes).atZone(zone).toInstant()
                day to Pair(dStart, dEnd)
            }
            weeklyAnalytics = AnalyticsEngine.calculateWeeklyAnalytics(sessions, now, boundaries)
        } else if (tab == AnalyticsTab.MONTHLY) {
            val yearMonth = YearMonth.from(date)
            val lengthOfMonth = yearMonth.lengthOfMonth()
            val boundaries = (1..lengthOfMonth).associate { i ->
                val day = yearMonth.atDay(i)
                val dStart = day.atTime(hours, minutes).atZone(zone).toInstant()
                val dEnd = day.plusDays(1).atTime(hours, minutes).atZone(zone).toInstant()
                day to Pair(dStart, dEnd)
            }
            monthlyAnalytics = AnalyticsEngine.calculateMonthlyAnalytics(sessions, now, yearMonth, boundaries)
        }

        AnalyticsUiState(
            selectedTab = tab,
            selectedDate = date,
            dailyAnalytics = dailyAnalytics,
            weeklyAnalytics = weeklyAnalytics,
            monthlyAnalytics = monthlyAnalytics,
            activities = activities
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    fun setTab(tab: AnalyticsTab) {
        _selectedTab.update { tab }
    }
    
    fun setDate(date: LocalDate) {
        _selectedDate.update { date }
    }
}
