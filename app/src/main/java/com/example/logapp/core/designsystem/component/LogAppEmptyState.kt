package com.example.logapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.logapp.core.designsystem.tokens.spacing

/**
 * Standard reusable Empty State presentation across all screens.
 */
@Composable
fun LogAppEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
    primaryButtonText: String? = null,
    onPrimaryClick: (() -> Unit)? = null,
    primaryButtonIcon: ImageVector? = null,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    LogAppElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(spacing.extraSmall))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (primaryButtonText != null && onPrimaryClick != null) {
                Spacer(modifier = Modifier.height(spacing.small))
                LogAppPrimaryButton(
                    text = primaryButtonText,
                    onClick = onPrimaryClick,
                    icon = primaryButtonIcon,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }

            if (secondaryButtonText != null && onSecondaryClick != null) {
                LogAppSecondaryButton(
                    text = secondaryButtonText,
                    onClick = onSecondaryClick,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}
