package com.example.logapp.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logapp.domain.engine.AnalyticsEngine
import com.example.logapp.domain.engine.DailyAnalytics
import com.example.logapp.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val dailyAnalytics: StateFlow<DailyAnalytics?> = sessionRepository.getAllSessions()
        .map { sessions ->
            AnalyticsEngine.calculateDailyAnalytics(sessions, Instant.now())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
