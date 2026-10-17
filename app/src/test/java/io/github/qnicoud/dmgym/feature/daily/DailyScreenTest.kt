package io.github.qnicoud.dmgym.feature.daily

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import io.github.qnicoud.dmgym.core.daily.ExerciseProgress
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.feature.home.ExerciseSummary
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class DailyScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<ExerciseId>()
    private val replaced = mutableListOf<Pair<Int, ExerciseId>>()
    private var backs = 0

    private val partial =
        DailyUiState(
            isLoading = false,
            epochDay = LocalDate.of(2026, 10, 16).toEpochDay(),
            items =
                listOf(
                    DailyItem(0, ExerciseId.CRAZY_EIGHTS, 480, ExerciseProgress.DONE),
                    DailyItem(1, ExerciseId.CONSTRAINT_BOX, 300, ExerciseProgress.IN_PROGRESS),
                    DailyItem(2, ExerciseId.DECISION_POINT, 30, ExerciseProgress.TODO),
                ),
            alternatives =
                listOf(
                    ExerciseSummary(ExerciseId.CORE_IMAGE, 600),
                    ExerciseSummary(ExerciseId.STATUS_NUMBERS, 180),
                    ExerciseSummary(ExerciseId.CONNECT_STORY, 600),
                    ExerciseSummary(ExerciseId.ONE_PAGE, null),
                ),
            nextExercise = ExerciseId.CONSTRAINT_BOX,
            started = true,
        )

    private val fresh =
        partial.copy(
            items = partial.items.map { it.copy(progress = ExerciseProgress.TODO) },
            nextExercise = ExerciseId.CRAZY_EIGHTS,
            started = false,
        )

    private val finished =
        partial.copy(
            items = partial.items.map { it.copy(progress = ExerciseProgress.DONE) },
            nextExercise = null,
            allDone = true,
        )

    private fun show(
        state: DailyUiState,
        dark: Boolean = false,
        fontScale: Float = 1f,
    ) {
        compose.setContent {
            DmGymTheme(darkTheme = dark) {
                WithFontScale(fontScale) {
                    DailyScreen(
                        state = state,
                        onBack = { backs++ },
                        onOpenExercise = { opened += it },
                        onReplace = { position, id -> replaced += position to id },
                    )
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

    @Test
    fun `shows the date, three exercises and the total duration`() {
        show(fresh)
        compose.onNodeWithTag(DailyTags.TITLE).assertTextEquals("Séance du 16 octobre")
        compose.onNodeWithTag(DailyTags.SUMMARY).assertTextEquals("3 exercices · ≈ 14 min")
        (0..2).forEach { compose.onNodeWithTag(DailyTags.row(it)).assertIsDisplayed() }
    }

    @Test
    fun `each row reads as one element with its state`() {
        show(partial)
        compose
            .onNodeWithTag(
                DailyTags.info(0),
            ).assertContentDescriptionContains("1. Crazy Eights, design thinking, 8 minutes, Fait")
        compose
            .onNodeWithTag(
                DailyTags.info(1),
            ).assertContentDescriptionContains("2. Constraint Box, jazz, 5 minutes, En cours")
        compose
            .onNodeWithTag(
                DailyTags.info(2),
            ).assertContentDescriptionContains("3. Decision Point Drill, science du sport, 30 secondes, À faire")
    }

    @Test
    fun `replace exists only on exercises still to do`() {
        show(partial)
        compose.onNodeWithTag(DailyTags.replace(0)).assertDoesNotExist()
        compose.onNodeWithTag(DailyTags.replace(1)).assertDoesNotExist()
        compose.onNodeWithTag(DailyTags.replace(2)).assertContentDescriptionContains("Remplacer Decision Point Drill")
    }

    @Test
    fun `the replace sheet lists the other exercises and choosing one replaces and closes`() {
        show(partial)
        compose.onNodeWithTag(DailyTags.replace(2)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(DailyTags.SHEET).assertIsDisplayed()
        ExerciseId.ALL
            .filter {
                it in
                    setOf(
                        ExerciseId.CORE_IMAGE,
                        ExerciseId.STATUS_NUMBERS,
                        ExerciseId.CONNECT_STORY,
                        ExerciseId.ONE_PAGE,
                    )
            }.forEach { compose.onNodeWithTag(DailyTags.choice(it)).assertIsDisplayed() }
        compose.onNodeWithTag(DailyTags.choice(ExerciseId.STATUS_NUMBERS)).performClick()
        compose.waitForIdle()
        assertEquals(listOf(2 to ExerciseId.STATUS_NUMBERS), replaced)
        compose.onNodeWithTag(DailyTags.SHEET).assertDoesNotExist()
    }

    @Test
    fun `start opens the first exercise and continue opens the one in progress`() {
        show(fresh)
        compose.onNodeWithTag(DailyTags.START).assertTextEquals("Commencer").performClick()
        assertEquals(listOf(ExerciseId.CRAZY_EIGHTS), opened)
    }

    @Test
    fun `continue is offered once the session is started`() {
        show(partial)
        compose.onNodeWithTag(DailyTags.START).assertTextEquals("Continuer").performClick()
        assertEquals(listOf(ExerciseId.CONSTRAINT_BOX), opened)
    }

    @Test
    fun `a finished session says so and offers to redo an exercise`() {
        show(finished)
        compose.onNodeWithTag(DailyTags.FINISHED).assertTextEquals("Séance terminée")
        compose.onNodeWithTag(DailyTags.START).assertDoesNotExist()
        compose.onNodeWithTag(DailyTags.replace(0)).assertDoesNotExist()
        compose
            .onNodeWithTag(
                DailyTags.redo(1),
            ).assertContentDescriptionContains("Refaire Constraint Box")
            .performClick()
        assertEquals(listOf(ExerciseId.CONSTRAINT_BOX), opened)
    }

    @Test
    fun `the back arrow leaves the screen`() {
        show(fresh)
        compose.onNodeWithContentDescription("Retour").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun `nothing is listed while loading`() {
        show(DailyUiState())
        compose.onNodeWithTag(DailyTags.row(0)).assertDoesNotExist()
        compose.onNodeWithTag(DailyTags.START).assertDoesNotExist()
    }

    @Test
    fun `renders in dark theme`() {
        show(partial, dark = true)
        compose.onNodeWithTag(DailyTags.row(2)).assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp")
    fun `stays usable at 200 percent font and keeps the start button reachable`() {
        show(fresh, fontScale = 2f)
        compose.onNodeWithTag(DailyTags.START).assertIsDisplayed()
        compose.onNodeWithTag(DailyTags.replace(2)).performScrollTo().assertIsDisplayed()
    }
}
