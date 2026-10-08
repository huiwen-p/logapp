package com.example.logapp.domain.controller

interface NotificationController {
    fun showActiveSessionNotification(sessionId: String, activityName: String, startedAt: Long)
    fun hideActiveSessionNotification()
}
