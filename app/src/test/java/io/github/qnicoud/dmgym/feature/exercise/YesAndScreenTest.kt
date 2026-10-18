package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.exercise.definitions.YesAnd
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Touche d'action du clavier de « Oui, et… » : Suivant passe à l'offre suivante, Terminé ferme le clavier. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class YesAndScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun launch(block: Boolean = false): ExerciseViewModel {
        val vm =
            ExerciseViewModel(
                definition = YesAnd.definition,
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
                launch = ExerciseLaunch(seed = 5),
            )
        compose.setContent { DmGymTheme { ExerciseRoute(vm, onBack = {}) } }
        compose.waitForIdle()
        if (block) vm.onOptionChanged(YesAnd.BLOCK_FIRST, true)
        compose.waitForIdle()
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
        return vm
    }

    private fun ime(
        field: String,
        offer: Int,
    ) {
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(field, offer))).performScrollTo().performImeAction()
        compose.waitForIdle()
    }

    @Test
    fun `next on the answer moves to the next offer`() {
        val vm = launch()
        ime(YesAnd.YES_AND_FIELD, 0)
        assertEquals(1, vm.uiState.value.session.currentQuestion)
    }

    @Test
    fun `next on the reflex goes to the answer, not to the next offer`() {
        val vm = launch(block = true)
        ime(YesAnd.BLOCK_FIELD, 0)
        assertEquals(0, vm.uiState.value.session.currentQuestion)
        ime(YesAnd.YES_AND_FIELD, 0)
        assertEquals(1, vm.uiState.value.session.currentQuestion)
    }

    @Test
    fun `done on the fifth offer neither advances nor finishes`() {
        val vm = launch()
        repeat(YesAnd.OFFER_COUNT - 1) { ime(YesAnd.YES_AND_FIELD, it) }
        assertEquals(YesAnd.OFFER_COUNT - 1, vm.uiState.value.session.currentQuestion)
        ime(YesAnd.YES_AND_FIELD, YesAnd.OFFER_COUNT - 1)
        assertEquals(YesAnd.OFFER_COUNT - 1, vm.uiState.value.session.currentQuestion)
        assertEquals(Phase.Active, vm.uiState.value.session.phase)
    }
}
