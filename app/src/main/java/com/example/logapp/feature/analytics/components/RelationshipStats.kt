package com.example.logapp.feature.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logapp.data.local.entity.ActivityEntity
import kotlin.time.Duration

@Composable
fun RelationshipStats(
    relationships: Map<Pair<String, String>, Duration>,
    activities: List<ActivityEntity>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Activity Relationships",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (relationships.isEmpty()) {
            Text(
                "Chưa có dữ liệu overlap (No parallel sessions)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val sortedRelationships = relationships.entries.sortedByDescending { it.value }
                    
                    sortedRelationships.forEach { (pair, duration) ->
                        val activityA = activities.find { it.id == pair.first }
                        val activityB = activities.find { it.id == pair.second }
                        
                        val nameA = activityA?.name ?: "Unknown"
                        val nameB = activityB?.name ?: "Unknown"
                        
                        val colorA = activityA?.color?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.primary
                        val colorB = activityB?.color?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.secondary
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(colorA))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(nameA, fontWeight = FontWeight.Medium, maxLines = 1)
                                
                                Text(" + ", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
                                
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(colorB))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(nameB, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                formatDuration(duration),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(duration: Duration): String {
    duration.toComponents { hours, minutes, seconds, _ ->
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
