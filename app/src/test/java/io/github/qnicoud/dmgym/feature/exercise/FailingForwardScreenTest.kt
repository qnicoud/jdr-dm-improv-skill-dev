package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.definitions.FailingForward
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Écran Prêt d'Échec qui avance : phrase d'aide et définition de chacun des 3 types tirés (parcours v1.1 §1.2). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class FailingForwardScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun launch() {
        val vm =
            ExerciseViewModel(
                definition = FailingForward.definition,
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

    @Test
    fun `ready shows the help sentence and one definition per drawn type`() {
        launch()
        compose.onNodeWithTag(ExerciseExtraTags.BOUND_HINT).assertExists()
        compose.onAllNodesWithTag(ExerciseExtraTags.BOUND_HELP).assertCountEquals(3)
    }

    @Test
    fun `the type definitions are gone once the exercise started`() {
        launch()
        runCatching { compose.onNodeWithTag(ExerciseTags.START).performScrollTo() }
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
        compose.onAllNodesWithTag(ExerciseExtraTags.BOUND_HELP).assertCountEquals(0)
    }
}
