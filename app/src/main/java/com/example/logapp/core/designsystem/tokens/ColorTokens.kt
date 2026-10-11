package com.example.logapp.core.designsystem.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic status colors supporting both Light and Dark themes according to Material 3.
 */
data class SemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color
)

val LightSemanticColors = SemanticColors(
    success = Color(0xFF2E7D32),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFC8E6C9),
    onSuccessContainer = Color(0xFF1B5E20),
    warning = Color(0xFFED6C02),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFE0B2),
    onWarningContainer = Color(0xFFE65100),
    info = Color(0xFF0288D1),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFB3E5FC),
    onInfoContainer = Color(0xFF01579B)
)

val DarkSemanticColors = SemanticColors(
    success = Color(0xFF81C784),
    onSuccess = Color(0xFF00390F),
    successContainer = Color(0xFF1B5E20),
    onSuccessContainer = Color(0xFFA5D6A7),
    warning = Color(0xFFFFB74D),
    onWarning = Color(0xFF4A2800),
    warningContainer = Color(0xFF7A3600),
    onWarningContainer = Color(0xFFFFCC80),
    info = Color(0xFF4FC3F7),
    onInfo = Color(0xFF00344F),
    infoContainer = Color(0xFF01579B),
    onInfoContainer = Color(0xFF81D4FA)
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

val semanticColors: SemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSemanticColors.current
