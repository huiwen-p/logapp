package com.example.logapp.feature.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LogAppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDanger: Boolean = false
) {
    if (isDanger) {
        com.example.logapp.core.designsystem.component.LogAppDangerButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        )
    } else {
        com.example.logapp.core.designsystem.component.LogAppPrimaryButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        )
    }
}
