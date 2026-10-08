package com.example.logapp.domain.repository

interface AuthRepository {
    suspend fun signInAnonymously(): Result<String>
    fun getCurrentUserId(): String?
}
