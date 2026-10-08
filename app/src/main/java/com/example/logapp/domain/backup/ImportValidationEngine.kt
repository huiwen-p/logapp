package com.example.logapp.domain.backup

import com.example.logapp.domain.model.backup.BackupSchemaV1
import javax.inject.Inject

class ImportValidationEngine @Inject constructor() {
    
    fun validate(data: BackupSchemaV1) {
        if (data.version != 1) {
            throw IllegalArgumentException("Unsupported backup version: ${data.version}")
        }
        
        val activityIds = data.activities.map { it.id }.toSet()
        
        for (session in data.sessions) {
            // Check reference
            if (!activityIds.contains(session.activityId)) {
                throw IllegalArgumentException("Session ${session.id} references unknown activity ${session.activityId}")
            }
            
            // Check timestamps
            if (session.endedAt != null && session.startedAt.isAfter(session.endedAt)) {
                throw IllegalArgumentException("Session ${session.id} has start time after end time")
            }
        }
    }
}
