package io.github.qnicoud.dmgym.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import io.github.qnicoud.dmgym.ui.theme.staticColorScheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val log = mutableListOf<String>()

    private val actions =
        SettingsActions(
            onBack = { log += "back" },
            onOpenAbout = { log += "about" },
            onVibration = { log += "vibration=$it" },
            onSound = { log += "sound=$it" },
            onCrazyHaptic = { log += "crazy=$it" },
            onDailyOnePage = { log += "onepage=$it" },
            onPalette = { log += "palette=${it.name}" },
            onDecrease = { log += "dec=${it.value}" },
            onIncrease = { log += "inc=${it.value}" },
            onUntimed = { id, on -> log += "untimed=${id.value}:$on" },
        )

    private val state =
        SettingsUiState(
            isLoading = false,
            durations =
                ExerciseId.ALL.map {
                    DurationRow(
                        it,
                        seconds = 300,
                        minSeconds = 150,
                        maxSeconds = 3000,
                        untimed =
                            it == ExerciseId.ONE_PAGE,
                    )
                },
            vibration = true,
            sound = false,
            palette = ThemePalette.PARCHMENT,
            palettes = ThemePalette.entries,
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
    fun `shows the switches in their current state`() {
        show()
        compose.onNodeWithTag(SettingsTags.VIBRATION).assertIsOn()
        compose.onNodeWithTag(SettingsTags.SOUND).assertIsOff()
        compose.onNodeWithTag(SettingsTags.CRAZY_HAPTIC).performScrollTo().assertIsOff()
        compose.onNodeWithTag(SettingsTags.DAILY_ONE_PAGE).performScrollTo().assertIsOff()
    }

    @Test
    fun `a switch row is read with its help text and its state`() {
        show()
        compose
            .onNodeWithTag(SettingsTags.SOUND)
            .assert(hasText("Bip de fin"))
            .assert(hasText("Ne sonne que si le téléphone est en mode sonnerie."))
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Désactivé"),
            )
        compose
            .onNodeWithTag(SettingsTags.VIBRATION)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Activé"))
    }

    @Test
    fun `toggling a row reports the new value`() {
        show()
        compose.onNodeWithTag(SettingsTags.VIBRATION).performClick()
        compose.onNodeWithTag(SettingsTags.SOUND).performClick()
        compose.onNodeWithTag(SettingsTags.CRAZY_HAPTIC).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTags.DAILY_ONE_PAGE).performScrollTo().performClick()
        assertEquals(listOf("vibration=false", "sound=true", "crazy=true", "onepage=true"), log)
    }

    @Test
    fun `duration steppers report the exercise and are described for TalkBack`() {
        show()
        compose.onNodeWithTag(SettingsTags.increase(ExerciseId.CRAZY_EIGHTS)).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTags.decrease(ExerciseId.CRAZY_EIGHTS)).performScrollTo().performClick()
        compose.onNodeWithContentDescription("Augmenter la durée de Crazy Eights").assertExists()
        compose.onNodeWithContentDescription("Réduire la durée de Crazy Eights").assertExists()
        assertEquals(listOf("inc=crazy_eights", "dec=crazy_eights"), log)
        compose.onNodeWithTag(SettingsTags.value(ExerciseId.CRAZY_EIGHTS)).assertTextEquals("5 min")
    }

    @Test
    fun `an untimed exercise shows no duration and disables its steppers`() {
        show()
        val onePage = ExerciseId.ONE_PAGE
        compose.onNodeWithTag(SettingsTags.value(onePage)).performScrollTo().assertTextEquals("Sans minuteur")
        compose.onNodeWithTag(SettingsTags.increase(onePage)).assertIsNotEnabled()
        compose.onNodeWithTag(SettingsTags.decrease(onePage)).assertIsNotEnabled()
        compose
            .onNodeWithTag(SettingsTags.untimed(onePage))
            .performScrollTo()
            .assertIsOn()
            .performClick()
        compose.onNodeWithTag(SettingsTags.increase(ExerciseId.CRAZY_EIGHTS)).performScrollTo().assertIsEnabled()
        assertEquals(listOf("untimed=one_page:false"), log)
    }

    @Test
    fun `steppers are disabled at the bounds`() {
        val atMin = state.durations.map { if (it.id == ExerciseId.CORE_IMAGE) it.copy(seconds = it.minSeconds) else it }
        show(state.copy(durations = atMin))
        compose.onNodeWithTag(SettingsTags.decrease(ExerciseId.CORE_IMAGE)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag(SettingsTags.increase(ExerciseId.CORE_IMAGE)).assertIsEnabled()
    }

    @Test
    fun `lists the palettes and reports the chosen one`() {
        show(state.copy(palette = ThemePalette.NIGHT))
        compose.onNodeWithTag(SettingsTags.palette(ThemePalette.NIGHT)).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(SettingsTags.palette(ThemePalette.FOREST)).performScrollTo().performClick()
        assertEquals(listOf("palette=FOREST"), log)
    }

    @Test
    fun `dynamic colors are hidden when they are not offered`() {
        show(state.copy(palettes = ThemePalette.available(sdkInt = 30)))
        compose.onNodeWithTag(SettingsTags.palette(ThemePalette.MIST)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.palette(ThemePalette.DYNAMIC)).assertDoesNotExist()
    }

    @Test
    fun `shows the dictation hint and opens the about screen`() {
        show()
        compose
            .onNodeWithTag(SettingsTags.DICTATION_HINT)
            .performScrollTo()
            .assertTextEquals("Pour dicter, utilise le micro de ton clavier.")
        compose.onNodeWithTag(SettingsTags.ABOUT).performScrollTo().performClick()
        compose.onNodeWithContentDescription("Retour").performClick()
        assertEquals(listOf("about", "back"), log)
    }

    @Test
    fun `touch targets are at least 48 dp`() {
        show()
        compose.onNodeWithTag(SettingsTags.VIBRATION).assertHeightIsAtLeast(48.dp)
        compose
            .onNodeWithTag(SettingsTags.increase(ExerciseId.CRAZY_EIGHTS))
            .performScrollTo()
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(48.dp)
        compose.onNodeWithTag(SettingsTags.palette(ThemePalette.FOREST)).performScrollTo().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag(SettingsTags.ABOUT).performScrollTo().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `stays usable at 200 percent font`() {
        show(fontScale = 2f)
        compose.onNodeWithTag(SettingsTags.VIBRATION).assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.increase(ExerciseId.DECISION_POINT)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.untimed(ExerciseId.DECISION_POINT)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.palette(ThemePalette.DYNAMIC)).performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithTag(SettingsTags.ABOUT)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(listOf("about"), log)
    }

    @Test
    fun `the theme follows the chosen palette`() {
        var primary = Color.Unspecified
        compose.setContent {
            DmGymTheme(darkTheme = false, palette = ThemePalette.FOREST) { primary = MaterialTheme.colorScheme.primary }
        }
        assertEquals(staticColorScheme(ThemePalette.FOREST, dark = false).primary, primary)
    }

    @Test
    @Config(sdk = [30])
    fun `dynamic falls back to parchment on API 30`() {
        var primary = Color.Unspecified
        compose.setContent {
            DmGymTheme(
                darkTheme = false,
                palette = ThemePalette.DYNAMIC,
            ) { primary = MaterialTheme.colorScheme.primary }
        }
        assertEquals(staticColorScheme(ThemePalette.PARCHMENT, dark = false).primary, primary)
    }
}
