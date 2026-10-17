package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.Milestone
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerKind
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
abstract class ExerciseViewModelTestBase {
    protected val dispatcher = UnconfinedTestDispatcher()
    protected val history = RecordingHistoryRepository()
    protected val settings = FakeSettingsRepository(Settings())
    protected val endSignal = FakeEndSignal()
    protected val monotonic = FakeMonotonicClock(1_000_000)
    protected val wall = MutableClock()
    protected val source = DemoExercise.FixtureSource()
    protected var detector: io.github.qnicoud.dmgym.core.exercise.AnswerDetector? = null
    protected var handle = SavedStateHandle()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    protected fun deps() =
        ExerciseDependencies(
            DemoExercise.tables(source),
            history,
            settings,
            endSignal,
            monotonic,
            wall,
            dispatcher,
            detector,
        )

    protected fun vm(
        definition: ExerciseDefinition = DemoExercise.definition,
        launch: ExerciseLaunch = ExerciseLaunch(seed = 7),
    ) = ExerciseViewModel(definition, deps(), handle, launch)

    /** Garde l'écran « collecté » pour que les tics tournent. */
    protected fun TestScope.collect(vm: ExerciseViewModel): Job {
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        val job = backgroundScope.launch(dispatcher) { vm.timerState.collect {} }
        runCurrent()
        return job
    }

    protected fun TestScope.pass(ms: Long) {
        monotonic.advance(ms)
        wall.millis += ms
        advanceTimeBy(TICK)
        runCurrent()
    }

    protected fun TestScope.started(definition: ExerciseDefinition = DemoExercise.definition): ExerciseViewModel {
        val vm = vm(definition)
        collect(vm)
        vm.onStart()
        runCurrent()
        return vm
    }

    protected companion object {
        const val TICK = 250L
    }
}
