package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import io.github.qnicoud.dmgym.accessibility.AccessibilityAudit
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class ReadyDurationSheetTest {
    @get:Rule
    val compose = createComposeRule()

    private var setting by mutableStateOf(
        TimerSetting(seconds = 300, minSeconds = 150, maxSeconds = 3_000, untimed = false),
    )
    private var fontScale by mutableFloatStateOf(1f)
    private val calls = mutableListOf<String>()

    private fun show(dark: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DmGymTheme(darkTheme = dark) {
                    DurationSheetContent(
                        exerciseName = "Constraint Box",
                        setting = setting,
                        onDecrease = { calls += "decrease" },
                        onIncrease = { calls += "increase" },
                        onUntimedChange = { calls += "untimed:$it" },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `shows the current duration and forwards the three actions`() {
        show()
        compose.onNodeWithTag(DurationSheetTags.VALUE).assertTextContains("5", substring = true)
        compose.onNodeWithTag(DurationSheetTags.DECREASE).performClick()
        compose.onNodeWithTag(DurationSheetTags.INCREASE).performClick()
        compose.onNodeWithTag(DurationSheetTags.UNTIMED).performClick()
        assertEquals(listOf("decrease", "increase", "untimed:true"), calls)
    }

    @Test
    fun `the steppers stop at the bounds`() {
        setting = setting.copy(seconds = 150)
        show()
        compose.onNodeWithTag(DurationSheetTags.DECREASE).assertIsNotEnabled()
        compose.onNodeWithTag(DurationSheetTags.INCREASE).assertIsEnabled()
        setting = setting.copy(seconds = 3_000)
        compose.waitForIdle()
        compose.onNodeWithTag(DurationSheetTags.INCREASE).assertIsNotEnabled()
    }

    @Test
    fun `the steppers are off while the exercise is untimed`() {
        setting = setting.copy(untimed = true)
        show()
        compose.onNodeWithTag(DurationSheetTags.DECREASE).assertIsNotEnabled()
        compose.onNodeWithTag(DurationSheetTags.INCREASE).assertIsNotEnabled()
        compose.onNodeWithTag(DurationSheetTags.UNTIMED).assertIsEnabled()
    }

    @Test
    fun `targets are at least 48 dp, labelled and not clipped at normal and double font scale`() {
        show(dark = true)
        val audit = AccessibilityAudit(compose)
        val problems = mutableListOf<String>()
        listOf(1f, 2f).forEach { scale ->
            fontScale = scale
            compose.waitForIdle()
            problems += audit.smallTargets() + audit.unlabelledClickables() + audit.statefulWithoutRole()
            if (scale == 2f) problems += audit.clippedTexts()
            compose.onNodeWithTag(DurationSheetTags.UNTIMED).assertIsDisplayed()
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `the duration is spoken as a live state description`() {
        show()
        val node = compose.onNodeWithTag(DurationSheetTags.VALUE).fetchSemanticsNode().config
        val state = node.getOrNull(SemanticsProperties.StateDescription).orEmpty()
        assertTrue(state, state.contains("5"))
        assertEquals(LiveRegionMode.Polite, node.getOrNull(SemanticsProperties.LiveRegion))
        setting = setting.copy(seconds = 360)
        compose.waitForIdle()
        val changed =
            compose
                .onNodeWithTag(DurationSheetTags.VALUE)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.StateDescription)
                .orEmpty()
        assertTrue(changed, changed.contains("6"))
    }
}
