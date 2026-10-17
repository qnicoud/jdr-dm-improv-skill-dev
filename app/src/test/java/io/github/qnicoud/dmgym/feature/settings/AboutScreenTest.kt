package io.github.qnicoud.dmgym.feature.settings

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class AboutScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var backs = 0

    @Test
    fun `shows the version and the font and beep licenses`() {
        compose.setContent { DmGymTheme { AboutScreen("0.1.0", onBack = { backs++ }) } }
        compose.onNodeWithTag(AboutTags.VERSION).assertTextEquals("Version 0.1.0")
        compose.onNodeWithText("Alegreya", substring = true).performScrollTo()
        compose.onNodeWithText("Atkinson Hyperlegible Next", substring = true).performScrollTo()
        compose.onNodeWithText("domaine public", substring = true).performScrollTo()
    }

    @Test
    fun `back button leaves the screen`() {
        compose.setContent { DmGymTheme { AboutScreen("0.1.0", onBack = { backs++ }) } }
        compose.onNodeWithContentDescription("Retour").performClick()
        assertEquals(1, backs)
    }
}
