package com.example.logapp.domain.repository

interface GoogleAuthRepository {
    fun getAccessToken(): String?
    fun isSignedIn(): Boolean
    fun signOut()
}
