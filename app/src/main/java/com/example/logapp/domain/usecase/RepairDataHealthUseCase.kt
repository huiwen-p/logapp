package com.example.logapp.domain.usecase

import com.example.logapp.domain.model.health.DataHealthReport
import com.example.logapp.domain.repository.DataHealthRepository
import javax.inject.Inject

class RepairDataHealthUseCase @Inject constructor(
    private val repository: DataHealthRepository
) {
    suspend operator fun invoke(): DataHealthReport {
        return repository.repairHealth()
    }
}
