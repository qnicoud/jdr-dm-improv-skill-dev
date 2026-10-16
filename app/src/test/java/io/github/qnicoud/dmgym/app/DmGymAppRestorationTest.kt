package io.github.qnicoud.dmgym.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.qnicoud.dmgym.feature.home.HomeTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** La pile de retour doit survivre à une recréation de l'Activity (rotation, mort du processus). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class DmGymAppRestorationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `back stack survives state restoration`() {
        val tester = StateRestorationTester(compose)
        tester.setContent { DmGymApp(FakeAppContainer()) }

        compose.onNodeWithTag(HomeTags.SETTINGS_BUTTON).performClick()
        compose.onNodeWithText("Réglages").assertIsDisplayed()

        tester.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Réglages").assertIsDisplayed()
    }
}
