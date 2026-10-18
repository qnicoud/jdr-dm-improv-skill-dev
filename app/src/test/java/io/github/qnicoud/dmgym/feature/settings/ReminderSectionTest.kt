package io.github.qnicoud.dmgym.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class ReminderSectionTest {
    @get:Rule
    val compose = createComposeRule()

    private val log = mutableListOf<String>()
    private val actions =
        ReminderActions(
            onSwitch = { log += "switch=$it" },
            onTime = { h, m -> log += "time=$h:$m" },
            onToggleDay = { log += "day=${it.name}" },
            onResume = { log += "resume" },
            onSendTest = { log += "test" },
            onOpenAndroidSettings = { log += "android" },
            onOpenChannelSettings = { log += "channel" },
        )

    private fun show(
        state: ReminderUiState,
        fontScale: Float = 1f,
    ) {
        compose.setContent {
            DmGymTheme {
                WithFontScale(fontScale) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        ReminderSection(state, actions, is24Hour = true)
                    }
                }
            }
        }
    }

    @Composable
    private fun WithFontScale(
        scale: Float,
        content: @Composable () -> Unit,
    ) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, scale), content = content)
    }

    private val on = ReminderUiState(isLoading = false, enabled = true)
    private val off = ReminderUiState(isLoading = false)

    @Test
    fun `off shows the summary and the permanent help, and hides the details`() {
        show(off)

        compose.onNodeWithTag(ReminderTags.SWITCH).assertIsDisplayed().assert(hasText("Désactivé"))
        compose.onNodeWithTag(ReminderTags.HELP).assertTextContains("Rien ne quitte ton téléphone.", substring = true)
        compose.onNodeWithTag(ReminderTags.TIME).assertDoesNotExist()
        compose.onNodeWithTag(ReminderTags.day(DayOfWeek.MONDAY)).assertDoesNotExist()
        compose.onNodeWithTag(ReminderTags.SEND_TEST).assertDoesNotExist()
    }

    @Test
    fun `tapping the switch asks to turn it on`() {
        show(off)

        compose.onNodeWithTag(ReminderTags.SWITCH).performClick()

        assertEquals(listOf("switch=true"), log)
    }

    @Test
    fun `on shows days and time in the summary and the details`() {
        show(on)

        compose.onNodeWithTag(ReminderTags.SWITCH).assert(hasText("Lun, mer, ven · vers 18 h 30"))
        compose.onNodeWithTag(ReminderTags.TIME).assertIsDisplayed()
        compose.onNodeWithTag(ReminderTags.SEND_TEST).assertIsDisplayed()
        compose.onNodeWithTag(ReminderTags.CHANNEL).assertIsDisplayed()
    }

    @Test
    fun `denied shows a neutral message with a link to Android settings and no repeated request`() {
        show(off.copy(permissionDenied = true))

        compose.onNodeWithTag(ReminderTags.DENIED).assertTextContains("refusées pour DM Gym", substring = true)
        compose.onNodeWithTag(ReminderTags.OPEN_ANDROID).assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(listOf("android"), log)
    }

    @Test
    fun `blocked while enabled says paused in text and keeps the switch on`() {
        show(on.copy(blocked = true))

        compose.onNodeWithTag(ReminderTags.BLOCKED).assertTextContains("Rappel en pause", substring = true)
        compose.onNodeWithTag(ReminderTags.OPEN_ANDROID).assertIsDisplayed()
        compose.onNodeWithTag(ReminderTags.SWITCH).assertIsOn()
    }

    @Test
    fun `fourteen days paused offers to resume`() {
        show(on.copy(paused = true))

        compose
            .onNodeWithTag(
                ReminderTags.PAUSED,
            ).assertTextContains("aucune session depuis 14 jours", substring = true)
        compose.onNodeWithTag(ReminderTags.RESUME).performClick()

        assertEquals(listOf("resume"), log)
    }

    @Test
    fun `day buttons are 48 dp, named in full and expose their state`() {
        show(on)

        DayOfWeek.entries.forEach {
            compose
                .onNodeWithTag(ReminderTags.day(it))
                .performScrollTo()
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
        compose.onNodeWithTag(ReminderTags.day(DayOfWeek.MONDAY)).assertContentDescriptionEquals("Lundi").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Sélectionné"),
        )
        compose.onNodeWithTag(ReminderTags.day(DayOfWeek.TUESDAY)).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Non sélectionné"),
        )
    }

    @Test
    fun `tapping a day toggles it`() {
        show(on)

        compose.onNodeWithTag(ReminderTags.day(DayOfWeek.SATURDAY)).performScrollTo().performClick()

        assertEquals(listOf("day=SATURDAY"), log)
    }

    @Test
    fun `the last day hint is neutral text`() {
        show(on.copy(keepOneDayHint = true))

        compose.onNodeWithTag(ReminderTags.KEEP_ONE_DAY).performScrollTo().assertTextEquals(
            "Garde au moins un jour, ou coupe le rappel avec l’interrupteur.",
        )
    }

    @Test
    fun `send test and channel link are actions`() {
        show(on)

        compose
            .onNodeWithTag(ReminderTags.SEND_TEST)
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose
            .onNodeWithTag(ReminderTags.CHANNEL)
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(listOf("test", "channel"), log)
    }

    @Test
    fun `time row opens the dialog and confirming reports the time`() {
        show(on)

        compose.onNodeWithTag(ReminderTags.TIME).assert(hasText("18 h 30")).performClick()
        compose.onNodeWithText("Heure du rappel").assertIsDisplayed()
        compose.onNodeWithTag(ReminderTags.TIME_OK).performClick()

        assertEquals(listOf("time=18:30"), log)
    }

    @Test
    fun `at 200 percent nothing is clipped and day buttons stay reachable`() {
        show(on, fontScale = 2f)

        compose.onNodeWithTag(ReminderTags.HELP).performScrollTo().assertIsDisplayed()
        DayOfWeek.entries.forEach {
            compose.onNodeWithTag(ReminderTags.day(it)).performScrollTo().assertIsDisplayed()
        }
    }
}
