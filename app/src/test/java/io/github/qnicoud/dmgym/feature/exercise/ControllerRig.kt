package io.github.qnicoud.dmgym.feature.exercise

import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.SlotDrawer
import io.github.qnicoud.dmgym.core.exercise.reduce
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/** Assemble les trois contrôleurs sur des doublures légères, sans ViewModel. */
internal class ControllerRig(
    scope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    historyOverride: ((RecordingHistoryRepository) -> HistoryRepository)? = null,
    val definition: ExerciseDefinition = DemoExercise.definition,
) {
    val history = RecordingHistoryRepository()
    val settings = FakeSettingsRepository(Settings())
    val endSignal = FakeEndSignal()
    val monotonic = FakeMonotonicClock(1_000_000)
    val wall = MutableClock()
    val handle = SavedStateHandle()
    val channel = Channel<ExerciseEvent>(Channel.BUFFERED)
    val engine = TimerEngine()
    val model =
        MutableStateFlow(ExerciseModel(ExerciseSession(definition.id), TimerState.Idle(DEFAULT_MS), loading = false))
    private val random = Random(1)
    private val deps =
        ExerciseDependencies(
            DemoExercise.tables(),
            historyOverride?.invoke(history) ?: history,
            settings,
            endSignal,
            monotonic,
            wall,
            ioDispatcher,
            null,
        )
    private val drawer = SlotDrawer(deps.tables, random)
    val timer =
        TimerController(
            engine,
            deps,
            handle,
            model,
            ScheduledEventRunner(
                engine,
                definition.events,
                random,
                drawer,
                scope,
                channel,
                ::dispatch,
                options = { emptyMap() },
                firedEvents = { model.value.session.firedEvents },
                draws = { model.value.session.draws },
            ),
            TimerAnnouncer(engine, deps, handle, scope, channel),
        )

    /** Texte déjà présent dans un champ à sa création, sans passer par le snapshot global de Compose. */
    val typed = mutableMapOf<io.github.qnicoud.dmgym.core.exercise.AnswerKey, String>()
    val fields =
        FieldStates(scope, {
            typed[it] ?: model.value.session.textOf(it)
        }, { k, t -> dispatch(ExerciseAction.AnswerChanged(k, t)) })
    val ready =
        ReadyController(
            definition,
            model,
            ReadySettingsWriter(definition.id, settings, scope, ioDispatcher) {
                val idle = model.value.timer as? TimerState.Idle
                ReadySetting((idle?.durationMs ?: DEFAULT_MS).milliseconds, model.value.untimed)
            },
            DrawCoordinator(definition, drawer, channel, ::dispatch) { model.value.session },
            DetectionCoordinator(definition, null, ::dispatch) { model.value.session },
            scope,
            ::dispatch,
        )
    val lifecycle =
        SessionLifecycle(
            definition,
            deps,
            handle,
            ExerciseLaunch(seed = 7),
            7,
            model,
            SessionRecorder(deps.history, wall, ioDispatcher),
            fields,
            ready,
            timer,
            scope,
            channel,
            ::dispatch,
        )

    fun dispatch(action: ExerciseAction) {
        model.update { it.copy(session = reduce(it.session, action)) }
    }

    companion object {
        const val DEFAULT_MS = 300_000L
    }
}
