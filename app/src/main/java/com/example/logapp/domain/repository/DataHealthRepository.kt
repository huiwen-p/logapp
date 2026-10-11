package com.example.logapp.domain.repository

import com.example.logapp.domain.model.health.DataHealthReport

interface DataHealthRepository {
    suspend fun checkHealth(): DataHealthReport
    suspend fun repairHealth(): DataHealthReport
}
