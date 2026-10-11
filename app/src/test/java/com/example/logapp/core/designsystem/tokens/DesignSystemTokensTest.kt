package com.example.logapp.core.designsystem.tokens

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemTokensTest {

    @Test
    fun testSpacingTokens_monotonicOrdering() {
        val spacing = Spacing()
        assertEquals(0.dp, spacing.none)
        assertTrue(spacing.extraSmall < spacing.small)
        assertTrue(spacing.small < spacing.medium)
        assertTrue(spacing.medium < spacing.large)
        assertTrue(spacing.large < spacing.extraLarge)
        assertTrue(spacing.extraLarge < spacing.huge)
    }

    @Test
    fun testSemanticColors_haveContrastAndDistinctValues() {
        val light = LightSemanticColors
        val dark = DarkSemanticColors

        // Ensure container and on-container colors are not identical (contrast check)
        assertNotEquals(light.successContainer, light.onSuccessContainer)
        assertNotEquals(light.warningContainer, light.onWarningContainer)
        assertNotEquals(light.infoContainer, light.onInfoContainer)

        assertNotEquals(dark.successContainer, dark.onSuccessContainer)
        assertNotEquals(dark.warningContainer, dark.onWarningContainer)
        assertNotEquals(dark.infoContainer, dark.onInfoContainer)
    }
}
