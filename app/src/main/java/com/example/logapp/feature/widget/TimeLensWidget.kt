package com.example.logapp.feature.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class TimeLensWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(16.dp).background(ColorProvider(android.graphics.Color.DKGRAY)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TimeLens Quick Track",
                    style = TextStyle(color = ColorProvider(android.graphics.Color.WHITE))
                )
                // In a real app, this would observe the Room DB or DataStore to show the current active session.
                // For simplicity in Phase 8, it provides a static entry point.
                Text(
                    text = "Open App to Track",
                    style = TextStyle(color = ColorProvider(android.graphics.Color.LTGRAY)),
                    modifier = GlanceModifier.padding(top = 8.dp)
                )
            }
        }
    }
}
