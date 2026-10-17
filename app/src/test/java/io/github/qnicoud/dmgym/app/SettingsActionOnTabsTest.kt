package io.github.qnicoud.dmgym.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.qnicoud.dmgym.ui.components.SettingsActionTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Remarque de recette 8.1 : l'engrenage des Réglages est dans la barre du haut des trois onglets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class SettingsActionOnTabsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `settings gear is on every tab with the same label and opens the settings`() {
        compose.setContent { DmGymApp(FakeAppContainer()) }
        listOf("S’entraîner", "Historique", "Progrès").forEach { tab ->
            compose.onNode(hasText(tab) and hasClickAction()).performClick()
            compose.onNodeWithTag(SettingsActionTag).assertIsDisplayed()
            compose.onNodeWithContentDescription("Réglages").assertIsDisplayed()
        }
        compose.onNodeWithTag(SettingsActionTag).performClick()
        compose.onNodeWithText("Apparence").assertIsDisplayed()
    }
}
