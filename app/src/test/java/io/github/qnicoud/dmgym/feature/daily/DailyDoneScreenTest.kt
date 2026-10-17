package io.github.qnicoud.dmgym.feature.daily

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import io.github.qnicoud.dmgym.core.daily.DailyDoneLine
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class DailyDoneScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var homes = 0
    private var redos = 0

    private val complete =
        DailyDoneUiState(
            isLoading = false,
            lines =
                listOf(
                    DailyDoneLine(ExerciseId.CRAZY_EIGHTS, true, 480, rating = 2, isGem = true),
                    DailyDoneLine(ExerciseId.CONSTRAINT_BOX, true, 300),
                    DailyDoneLine(ExerciseId.DECISION_POINT, true, 30, rating = 3),
                ),
            sessionCount = 48,
            streakDays = 6,
        )

    private fun show(
        state: DailyDoneUiState,
        dark: Boolean = false,
        fontScale: Float = 1f,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DmGymTheme(darkTheme = dark) {
                    DailyDoneScreen(state, onHome = { homes++ }, onRedo = { redos++ }, onBack = {})
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `shows the title, an encouraging message, the three exercises and the counters`() {
        show(complete)
        compose.onNodeWithTag(DailyDoneTags.TITLE).assertTextEquals("Séance terminée")
        compose.onNodeWithTag(DailyDoneTags.MESSAGE).assertIsDisplayed()
        (0..2).forEach { compose.onNodeWithTag(DailyDoneTags.line(it)).assertIsDisplayed() }
        compose.onNodeWithTag(DailyDoneTags.SESSIONS).assertTextEquals("48 sessions")
        compose.onNodeWithTag(DailyDoneTags.STREAK).assertTextEquals("Série : 6 jours")
    }

    @Test
    fun `each line shows its level as a label and its gem with a description`() {
        show(complete)
        compose.onAllNodes(hasText("Solide")).assertCountEquals(1)
        compose.onAllNodes(hasText("Prêt pour la table")).assertCountEquals(1)
        compose.onAllNodes(hasContentDescription("Bijou")).assertCountEquals(1)
    }

    @Test
    fun `a partial session stays kind and shows what was not done without blame`() {
        val partial =
            complete.copy(
                lines =
                    complete.lines.mapIndexed { i, l ->
                        if (i ==
                            2
                        ) {
                            DailyDoneLine(l.id, false)
                        } else {
                            l
                        }
                    },
            )
        show(partial)
        compose
            .onNodeWithTag(
                DailyDoneTags.MESSAGE,
            ).assertTextEquals("Chaque exercice compte. Tu reprendras quand tu voudras.")
        compose.onAllNodes(hasText("Pas cette fois")).assertCountEquals(1)
    }

    @Test
    fun `the streak is hidden when there is none`() {
        show(complete.copy(streakDays = 0))
        compose.onAllNodesWithTag(DailyDoneTags.STREAK).assertCountEquals(0)
    }

    @Test
    fun `back to home and redo call their actions`() {
        show(complete)
        compose.onNodeWithTag(DailyDoneTags.HOME).assertTextEquals("Retour à l’accueil").performClick()
        compose.onNodeWithTag(DailyDoneTags.REDO).performScrollTo().performClick()
        assertEquals(1, homes)
        assertEquals(1, redos)
    }

    @Test
    fun `nothing is shown while loading`() {
        show(DailyDoneUiState())
        compose.onAllNodesWithTag(DailyDoneTags.TITLE).assertCountEquals(0)
    }

    @Test
    fun `the exit stays reachable at 200 percent font in dark theme`() {
        show(complete, dark = true, fontScale = 2f)
        compose.onNodeWithTag(DailyDoneTags.HOME).performScrollTo().assertIsDisplayed()
    }
}
