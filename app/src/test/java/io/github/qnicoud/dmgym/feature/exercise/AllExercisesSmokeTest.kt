package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Fumée : chaque exercice du registre réel s'ouvre, démarre et accepte la saisie sans planter. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class AllExercisesSmokeTest {
    @get:Rule
    val compose = createComposeRule()

    private fun launch(definition: ExerciseDefinition): ExerciseViewModel {
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
        return vm
    }

    @Test
    fun `the real registry holds every exercise`() {
        assertEquals(
            ExerciseId.ALL.toSet(),
            ExerciseRegistry.Default.all
                .map { it.id }
                .toSet(),
        )
    }

    private fun smoke(id: ExerciseId) {
        val definition = requireNotNull(ExerciseRegistry.Default[id]) { "$id missing from registry" }
        val vm = launch(definition)
        runCatching { compose.onNodeWithTag(ExerciseTags.START).performScrollTo() }
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
        assertTrue("$id should leave Ready", vm.uiState.value.session.phase != Phase.Ready)
        // Status Numbers is played aloud: no input field during the scene.
        val firstKey = definition.fieldKeys.firstOrNull() ?: return
        val tag = ExerciseTags.field(AnswerKey(firstKey))
        if (compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag(tag).performScrollTo().performTextInput("une auberge")
            compose.waitForIdle()
        }
    }

    @Test
    fun `crazy eights opens, starts and accepts typing`() = smoke(ExerciseId.CRAZY_EIGHTS)

    @Test
    fun `constraint box opens, starts and accepts typing`() = smoke(ExerciseId.CONSTRAINT_BOX)

    @Test
    fun `connect the story opens, starts and accepts typing`() = smoke(ExerciseId.CONNECT_STORY)

    @Test
    fun `core image opens, starts and accepts typing`() = smoke(ExerciseId.CORE_IMAGE)

    @Test
    fun `status numbers opens, starts and accepts typing`() = smoke(ExerciseId.STATUS_NUMBERS)

    @Test
    fun `decision point opens, starts and accepts typing`() = smoke(ExerciseId.DECISION_POINT)

    @Test
    fun `one page opens, starts and accepts typing`() = smoke(ExerciseId.ONE_PAGE)

    @Test
    fun `three clues opens, starts and accepts typing`() = smoke(ExerciseId.THREE_CLUES)

    @Test
    fun `failing forward opens, starts and accepts typing`() = smoke(ExerciseId.FAILING_FORWARD)
}
