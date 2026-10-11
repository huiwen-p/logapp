package com.example.logapp.domain.repository

interface GoogleAuthRepository {
    fun getAccessToken(): String?
    fun getSignedInEmail(): String?
    fun isSignedIn(): Boolean
    fun signOut()
}
