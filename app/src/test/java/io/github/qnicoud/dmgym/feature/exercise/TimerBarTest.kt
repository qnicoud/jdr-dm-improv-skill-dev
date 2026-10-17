package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.qnicoud.dmgym.core.exercise.TimerKind
import io.github.qnicoud.dmgym.core.exercise.TimerUi
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** La barre se dessine sans erreur dans ses six états, y compris les extrémités arrondies (retours de test 1). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class TimerBarTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `every timer state draws its bar`() {
        var kind by mutableStateOf(TimerKind.Ready)
        compose.setContent {
            val timer = TimerUi(kind, displayMs = 90_000, fraction = 0.5f, overtime = kind == TimerKind.Overtime)
            DmGymTheme { TimerBar(timer, true, {}, {}) }
        }
        TimerKind.entries.forEach {
            kind = it
            compose.waitForIdle()
            compose.onNodeWithTag(ExerciseTags.TIMER).assertIsDisplayed()
        }
    }

    @Test
    fun `fill width never drops below the bar height so the round caps stay visible`() {
        assertEquals(10f, progressFillWidth(trackWidth = 100f, height = 10f, fraction = 0.01f))
        assertEquals(0f, progressFillWidth(trackWidth = 100f, height = 10f, fraction = 0f))
        assertEquals(100f, progressFillWidth(trackWidth = 100f, height = 10f, fraction = 1f))
        assertEquals(50f, progressFillWidth(trackWidth = 100f, height = 10f, fraction = 0.5f))
    }
}
