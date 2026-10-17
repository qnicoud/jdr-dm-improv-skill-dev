package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Recette 4.4 : en paysage (800 x 360 dp), la zone épinglée est compacte et le champ de saisie reste visible. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h360dp")
class ExerciseLandscapeTest {
    @get:Rule
    val compose = createComposeRule()

    private fun launch(definition: ExerciseDefinition) {
        val vm =
            ExerciseViewModel(
                definition = definition,
                deps =
                    ExerciseDependencies(
                        RealTables.repository,
                        RecordingHistoryRepository(),
                        FakeSettingsRepository(Settings()),
                        FakeEndSignal(),
                        FakeMonotonicClock(1_000_000),
                        MutableClock(),
                        Dispatchers.Unconfined,
                    ),
                handle = SavedStateHandle(),
                launch = ExerciseLaunch(seed = 11),
            )
        compose.setContent { DmGymTheme { ExerciseRoute(vm, onBack = {}) } }
        compose.waitForIdle()
    }

    private fun assertFieldVisible(definition: ExerciseDefinition) {
        val firstKey = definition.fieldKeys.firstOrNull() ?: return
        val tag = ExerciseTags.field(AnswerKey(firstKey))
        if (compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()) return
        val field = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val density = compose.density.density
        assertTrue("${definition.id}: field top ${field.top} below the screen ${root.bottom}", field.top < root.bottom)
        assertTrue(
            "${definition.id}: field shows only ${(root.bottom - field.top) / density} dp",
            root.bottom - field.top >= MIN_VISIBLE_DP * density,
        )
    }

    private fun check(id: ExerciseId) {
        val definition = requireNotNull(ExerciseRegistry.Default[id])
        launch(definition)
        runCatching { compose.onNodeWithTag(ExerciseTags.START).performClick() }
        compose.waitForIdle()
        assertFieldVisible(definition)
    }

    @Test
    fun `crazy eights keeps its field visible in landscape`() = check(ExerciseId.CRAZY_EIGHTS)

    @Test
    fun `constraint box keeps its field visible in landscape`() = check(ExerciseId.CONSTRAINT_BOX)

    @Test
    fun `core image keeps its field visible in landscape`() = check(ExerciseId.CORE_IMAGE)

    @Test
    fun `status numbers has no field to hide in landscape`() = check(ExerciseId.STATUS_NUMBERS)

    @Test
    fun `decision point keeps its field visible in landscape`() = check(ExerciseId.DECISION_POINT)

    @Test
    fun `connect the story keeps its field visible in landscape`() = check(ExerciseId.CONNECT_STORY)

    @Test
    fun `one page keeps its field visible in landscape`() = check(ExerciseId.ONE_PAGE)

    @Test
    fun `the draw is folded to one line, with a chevron to open it`() {
        launch(requireNotNull(ExerciseRegistry.Default[ExerciseId.CONSTRAINT_BOX]))
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(ExerciseTags.PINNED_SUMMARY).assertExists()
        compose.onAllNodesWithTag(ExerciseTags.PINNED_DETAILS).assertCountEquals(0)
        compose.onNodeWithTag(ExerciseTags.PINNED_SUMMARY).performClick()
        compose.onNodeWithTag(ExerciseTags.PINNED_DETAILS).assertExists()
    }

    private companion object {
        /** Environ 3 lignes de texte avec leurs marges. */
        const val MIN_VISIBLE_DP = 96
    }
}
