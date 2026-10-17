package io.github.qnicoud.dmgym.feature.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.streak.DayMark
import io.github.qnicoud.dmgym.core.streak.DayStatus
import io.github.qnicoud.dmgym.core.streak.StreakSummary
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class StatsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    // mercredi 14 octobre 2026
    private val today = LocalDate.of(2026, 10, 14).toEpochDay()
    private var chosen = 0
    private var gemsShown = 0

    private fun row(vararg marks: DayMark) = marks.mapIndexed { i, mark -> DayStatus(today - 6 + i, mark) }

    private val normal =
        StatsUiState(
            isLoading = false,
            today = today,
            sessions = 47,
            sessionsThisWeek = 6,
            streak = StreakSummary(47, 30, 5, 12, null, true),
            recent =
                row(
                    DayMark.PRACTICED,
                    DayMark.PRACTICED,
                    DayMark.NONE,
                    DayMark.PRACTICED,
                    DayMark.PRACTICED,
                    DayMark.PRACTICED,
                    DayMark.PRACTICED,
                ),
            perExercise = listOf(ExerciseCount(ExerciseId.CRAZY_EIGHTS, 12), ExerciseCount(ExerciseId.ONE_PAGE, 1)),
            gems = 9,
            totalSeconds = 5 * 3_600L + 12 * 60,
        )

    private fun show(
        state: StatsUiState,
        dark: Boolean = false,
        fontScale: Float = 1f,
    ) {
        compose.setContent {
            DmGymTheme(darkTheme = dark) {
                WithFontScale(fontScale) {
                    StatsScreen(state, onChooseExercise = { chosen++ }, onShowGems = { gemsShown++ })
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

    private fun top(tag: String) =
        compose
            .onNodeWithTag(tag)
            .fetchSemanticsNode()
            .boundsInRoot.top

    @Test
    fun `sessions are the first and largest figure`() {
        show(normal)
        compose.onNodeWithTag(StatsTags.SESSIONS).assertTextEquals("47 sessions")
        compose.onNodeWithTag(StatsTags.THIS_WEEK).assertTextEquals("dont 6 cette semaine")
        assertTrue(top(StatsTags.SESSIONS) < top(StatsTags.STREAK))
        assertTrue(top(StatsTags.STREAK) < top(StatsTags.BEST))
        assertTrue(top(StatsTags.BEST) < top(StatsTags.bar(ExerciseId.CRAZY_EIGHTS)))
    }

    @Test
    fun `the streak shows the grace day as available`() {
        show(normal)
        compose.onNodeWithTag(StatsTags.STREAK).assertTextEquals("Série : 5 jours")
        compose.onNodeWithTag(StatsTags.GRACE).assertTextEquals("Jour de grâce disponible")
        compose.onNodeWithTag(StatsTags.BEST).assertTextEquals("Meilleure série : 12 jours")
    }

    @Test
    fun `the streak says when the grace day was used this week`() {
        show(normal.copy(streak = normal.streak.copy(graceUsedOn = today - 1, graceDays = setOf(today - 1))))
        compose.onNodeWithTag(StatsTags.GRACE).assertTextEquals("Jour de grâce utilisé cette semaine · rechargé lundi")
    }

    @Test
    fun `the grace day is available again after the monday reload`() {
        show(normal.copy(streak = normal.streak.copy(graceUsedOn = today - 3)))
        compose.onNodeWithTag(StatsTags.GRACE).assertTextEquals("Jour de grâce disponible")
    }

    @Test
    fun `a broken streak is a calm new streak and keeps the best`() {
        show(normal.copy(streak = normal.streak.copy(current = 1)))
        compose.onNodeWithTag(StatsTags.STREAK).assertTextEquals("Nouvelle série : 1 jour")
        compose.onNodeWithTag(StatsTags.BEST).assertTextEquals("Meilleure série : 12 jours")
        compose.onNodeWithTag(StatsTags.SESSIONS).assertTextEquals("47 sessions")
    }

    @Test
    fun `without a current streak no zero is shown`() {
        show(normal.copy(streak = normal.streak.copy(current = 0)))
        compose.onNodeWithTag(StatsTags.STREAK).assertTextEquals("Une nouvelle série commence à ta prochaine session.")
        compose.onNodeWithTag(StatsTags.GRACE).assertDoesNotExist()
    }

    @Test
    fun `the recent row has seven days described for the screen reader`() {
        show(
            normal.copy(
                recent =
                    row(
                        DayMark.PRACTICED,
                        DayMark.GRACE,
                        DayMark.NONE,
                        DayMark.PRACTICED,
                        DayMark.PRACTICED,
                        DayMark.PRACTICED,
                        DayMark.PRACTICED,
                    ),
            ),
        )
        compose.onNodeWithTag(StatsTags.day(today)).assertContentDescriptionEquals("mercredi, aujourd’hui : pratiqué")
        compose.onNodeWithTag(StatsTags.day(today - 5)).assertContentDescriptionEquals("vendredi : jour de grâce")
        compose.onNodeWithTag(StatsTags.day(today - 4)).assertContentDescriptionEquals("samedi : pas de pratique")
        compose.onNodeWithTag(StatsTags.day(today - 6)).assertContentDescriptionEquals("jeudi : pratiqué")
    }

    @Test
    fun `each exercise bar has a text equivalent`() {
        show(normal)
        compose
            .onNodeWithTag(
                StatsTags.bar(ExerciseId.CRAZY_EIGHTS),
            ).assertContentDescriptionEquals("Crazy Eights : 12 sessions")
        compose
            .onNodeWithTag(
                StatsTags.bar(ExerciseId.ONE_PAGE),
            ).assertContentDescriptionEquals("One-Page Design : 1 session")
    }

    @Test
    fun `gems and time are shown and the gems link works`() {
        show(normal)
        compose.onNodeWithTag(StatsTags.GEMS).performScrollTo().assertTextEquals("9 bijoux")
        compose.onNodeWithTag(StatsTags.TIME).assertTextEquals("5 h 12 d’écriture")
        compose.onNodeWithTag(StatsTags.GEMS_LINK).performClick()
        assertEquals(1, gemsShown)
    }

    @Test
    fun `time under one hour is shown in minutes and zero gems and time are hidden`() {
        show(normal.copy(totalSeconds = 25 * 60L, gems = 0))
        compose.onNodeWithTag(StatsTags.TIME).assertTextEquals("25 min d’écriture")
        compose.onNodeWithTag(StatsTags.GEMS).assertDoesNotExist()
        compose.onNodeWithTag(StatsTags.GEMS_LINK).assertDoesNotExist()
    }

    @Test
    fun `the empty state shows a single action and no zeros`() {
        show(StatsUiState(isLoading = false))
        compose.onNodeWithTag(StatsTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithTag(StatsTags.SESSIONS).assertDoesNotExist()
        compose.onNodeWithTag(StatsTags.STREAK).assertDoesNotExist()
        compose.onNodeWithTag(StatsTags.EMPTY_ACTION).performClick()
        assertEquals(1, chosen)
    }

    @Test
    fun `nothing is shown while loading`() {
        show(StatsUiState())
        compose.onNodeWithTag(StatsTags.EMPTY).assertDoesNotExist()
        compose.onNodeWithTag(StatsTags.SESSIONS).assertDoesNotExist()
    }

    @Test
    fun `renders in dark theme`() {
        show(normal, dark = true)
        compose.onNodeWithTag(StatsTags.SESSIONS).assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp")
    fun `stays usable at 200 percent font`() {
        show(normal, fontScale = 2f)
        compose.onNodeWithTag(StatsTags.SESSIONS).assertIsDisplayed()
        compose.onNodeWithTag(StatsTags.STREAK).performScrollTo().assertIsDisplayed()
    }
}
