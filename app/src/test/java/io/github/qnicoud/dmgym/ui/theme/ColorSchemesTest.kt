package io.github.qnicoud.dmgym.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ColorSchemesTest {
    @Test
    fun `light scheme follows the parchment tokens`() {
        assertEquals(Color(0xFF8A2E1C), LightColors.primary)
        assertEquals(Color(0xFFFBF5EA), LightColors.background)
        assertEquals(Color(0xFFF6EFE2), LightColors.surfaceContainerLow)
        assertEquals(Color(0xFFFFE08A), LightColors.tertiaryContainer)
    }

    @Test
    fun `dark scheme follows the torch leather tokens`() {
        assertEquals(Color(0xFFFFB4A2), DarkColors.primary)
        assertEquals(Color(0xFF1B1612), DarkColors.background)
        assertEquals(Color(0xFF211B16), DarkColors.surfaceContainerLow)
        assertEquals(Color(0xFF5A4300), DarkColors.tertiaryContainer)
    }
}
