package com.example.logapp.feature.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.domain.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ManageActivitiesViewModel @Inject constructor(
    private val activityRepository: ActivityRepository
) : ViewModel() {

    val activities: StateFlow<List<ActivityEntity>> = activityRepository.getAllActivities()
        .map { list -> list.sortedBy { it.sortOrder } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveActivity(id: String?, name: String, color: Long, iconName: String) {
        viewModelScope.launch {
            if (id == null) {
                val currentMax = activities.value.maxOfOrNull { it.sortOrder } ?: -1
                val newActivity = ActivityEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    color = color,
                    icon = iconName,
                    sortOrder = currentMax + 1,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                activityRepository.addActivity(newActivity)
            } else {
                val existing = activities.value.find { it.id == id }
                if (existing != null) {
                    val updated = existing.copy(
                        name = name,
                        color = color,
                        icon = iconName,
                        updatedAt = Instant.now()
                    )
                    activityRepository.updateActivity(updated)
                }
            }
        }
    }

    fun archiveActivity(id: String, archive: Boolean = true) {
        viewModelScope.launch {
            val existing = activities.value.find { it.id == id }
            if (existing != null) {
                activityRepository.updateActivity(
                    existing.copy(
                        isArchived = archive,
                        updatedAt = Instant.now()
                    )
                )
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
