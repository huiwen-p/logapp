package com.example.logapp.domain.backup

import com.example.logapp.data.local.entity.ActivityEntity
import com.example.logapp.data.local.entity.ActivitySessionEntity
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CsvGenerator @Inject constructor() {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    fun generateSessionsCsv(sessions: List<ActivitySessionEntity>, activities: List<ActivityEntity>): String {
        val activityMap = activities.associateBy { it.id }
        
        val builder = java.lang.StringBuilder()
        builder.append("Session ID,Activity Name,Start Time,End Time,Duration (Minutes),Note\n")
        
        for (session in sessions.filter { it.deletedAt == null }) {
            val activityName = activityMap[session.activityId]?.name ?: "Unknown"
            val startStr = formatter.format(session.startedAt)
            val endStr = session.endedAt?.let { formatter.format(it) } ?: "Ongoing"
            
            val durationMins = if (session.endedAt != null) {
                java.time.Duration.between(session.startedAt, session.endedAt).toMinutes().toString()
            } else {
                ""
            }
            
            // Escape note for CSV if it contains commas, quotes, or newlines
            val rawNote = session.note ?: ""
            val escapedNote = if (rawNote.contains(",") || rawNote.contains("\"") || rawNote.contains("\n")) {
                "\"${rawNote.replace("\"", "\"\"")}\""
            } else {
                rawNote
            }
            
            builder.append("${session.id},$activityName,$startStr,$endStr,$durationMins,$escapedNote\n")
        }
        
        return builder.toString()
    }
}
