package com.example.logapp.service.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.logapp.domain.usecase.StopActivityUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StopSessionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var stopActivityUseCase: StopActivityUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(SessionNotificationService.EXTRA_SESSION_ID)
        if (sessionId != null) {
            scope.launch {
                stopActivityUseCase(sessionId)
                
                // Stop the service
                val stopServiceIntent = Intent(context, SessionNotificationService::class.java).apply {
                    action = SessionNotificationService.ACTION_STOP
                }
                context.startService(stopServiceIntent)
            }
        }
    }
}
