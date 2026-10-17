package io.github.qnicoud.dmgym.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.ThemeMode
import io.github.qnicoud.dmgym.core.settings.VibrationStrength
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Lot L12a : force des vibrations, thème, animations réduites, données. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class SettingsNewSectionsTest {
    @get:Rule
    val compose = createComposeRule()

    private val log = mutableListOf<String>()

    private val actions =
        SettingsActions(
            onVibrationStrength = { log += "strength=${it.name}" },
            onTryVibration = { log += "try" },
            onThemeMode = { log += "mode=${it.name}" },
            onReduceAnimations = { log += "reduce=$it" },
            onExport = { log += "export" },
            onRequestErase = { log += "request" },
            onCancelErase = { log += "cancel" },
            onConfirmErase = { log += "confirm" },
        )

    private val state =
        SettingsUiState(
            isLoading = false,
            durations = ExerciseId.ALL.map { DurationRow(it, 300, 150, 3000, untimed = false) },
        )

    private fun show(
        value: SettingsUiState = state,
        fontScale: Float = 1f,
    ) {
        compose.setContent {
            DmGymTheme {
                WithFontScale(fontScale) { SettingsScreen(value, actions) }
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

    @Test
    fun `the chosen vibration strength is selected and a click reports another one`() {
        show(state.copy(vibrationStrength = VibrationStrength.SOFT))
        compose.onNodeWithTag(SettingsTags.strength(VibrationStrength.SOFT)).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(SettingsTags.strength(VibrationStrength.STRONG)).performScrollTo().performClick()
        assertEquals(listOf("strength=STRONG"), log)
    }

    @Test
    fun `the try button plays the vibration and is described for TalkBack`() {
        show()
        compose
            .onNodeWithTag(SettingsTags.TRY_VIBRATION)
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assert(hasText("Essayer"))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Essayer la vibration à la force choisie"),
                ),
            ).performClick()
        assertEquals(listOf("try"), log)
    }

    @Test
    fun `the strength choice is disabled when vibration is off and hidden without a vibrator`() {
        show(state.copy(vibration = false))
        compose.onNodeWithTag(SettingsTags.strength(VibrationStrength.NORMAL)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag(SettingsTags.TRY_VIBRATION).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `without a vibrator there is no strength choice`() {
        show(state.copy(canVibrate = false))
        compose.onNodeWithTag(SettingsTags.strength(VibrationStrength.NORMAL)).assertDoesNotExist()
        compose.onNodeWithTag(SettingsTags.TRY_VIBRATION).assertDoesNotExist()
    }

    @Test
    fun `theme mode lists system light and dark`() {
        show(state.copy(themeMode = ThemeMode.DARK))
        compose.onNodeWithTag(SettingsTags.themeMode(ThemeMode.DARK)).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(SettingsTags.themeMode(ThemeMode.SYSTEM)).performScrollTo().assert(hasText("Système"))
        compose
            .onNodeWithTag(
                SettingsTags.themeMode(ThemeMode.LIGHT),
            ).performScrollTo()
            .assert(hasText("Clair"))
            .performClick()
        assertEquals(listOf("mode=LIGHT"), log)
    }

    @Test
    fun `reduce animations follows the phone by default and can be forced either way`() {
        show()
        compose.onNodeWithTag(SettingsTags.REDUCE_SYSTEM).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(SettingsTags.REDUCE_ALWAYS).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTags.REDUCE_NEVER).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTags.REDUCE_SYSTEM).performScrollTo().performClick()
        assertEquals(listOf("reduce=true", "reduce=false", "reduce=null"), log)
    }

    @Test
    fun `export is a button and says the data stays on the phone`() {
        show()
        compose
            .onNodeWithTag(
                SettingsTags.DATA_HINT,
            ).performScrollTo()
            .assert(hasText("restent sur ce téléphone", substring = true))
        compose
            .onNodeWithTag(SettingsTags.EXPORT)
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(listOf("export"), log)
    }

    @Test
    fun `erasing asks for a confirmation in the page before doing anything`() {
        show()
        compose.onNodeWithTag(SettingsTags.ERASE_CONFIRM).assertDoesNotExist()
        compose
            .onNodeWithTag(SettingsTags.ERASE)
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(listOf("request"), log)
    }

    @Test
    fun `the confirmation can be accepted or cancelled`() {
        show(state.copy(data = DataUiState(confirmingErase = true)))
        compose
            .onNodeWithTag(
                SettingsTags.ERASE_CONFIRM,
            ).performScrollTo()
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag(SettingsTags.ERASE_CONFIRM).performClick()
        compose
            .onNodeWithTag(SettingsTags.ERASE_CANCEL)
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(listOf("confirm", "cancel"), log)
    }

    @Test
    fun `the result of a data action is written in the page`() {
        show(state.copy(data = DataUiState(message = DataMessage.ERASED)))
        compose
            .onNodeWithTag(SettingsTags.DATA_MESSAGE)
            .performScrollTo()
            .assertTextEquals("Toutes les sessions ont été effacées.")
    }

    @Test
    fun `the new sections stay usable at 200 percent font`() {
        show(state.copy(data = DataUiState(confirmingErase = true)), fontScale = 2f)
        compose.onNodeWithTag(SettingsTags.strength(VibrationStrength.STRONG)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.TRY_VIBRATION).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.themeMode(ThemeMode.LIGHT)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.REDUCE_NEVER).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.EXPORT).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.ERASE_CONFIRM).performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithTag(SettingsTags.ERASE_CANCEL)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(listOf("cancel"), log)
    }
}
