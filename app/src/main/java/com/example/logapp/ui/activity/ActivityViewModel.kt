package com.example.logapp.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logapp.data.model.ActivityEntity
import com.example.logapp.data.repository.ActivityRepository
import com.example.logapp.utils.usecase.AddActivityUseCase
import com.example.logapp.utils.usecase.DeleteActivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val addActivityUseCase: AddActivityUseCase,
    private val deleteActivityUseCase: DeleteActivityUseCase
) : ViewModel() {

    val activities: StateFlow<List<ActivityEntity>> = activityRepository.getAllActivities()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addActivity(name: String) {
        viewModelScope.launch {
            val newActivity = ActivityEntity(
                name = name,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            addActivityUseCase(newActivity)
        }
    }

    fun deleteActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            deleteActivityUseCase(activity)
            // Error handling (e.g. show toast if Result is failure) can be implemented here
        }
    }
}
