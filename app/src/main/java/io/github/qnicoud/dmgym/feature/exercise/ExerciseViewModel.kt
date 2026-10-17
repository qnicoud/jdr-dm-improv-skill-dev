package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.DueEvent
import io.github.qnicoud.dmgym.core.exercise.EventPlan
import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.Milestone
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.ScheduledEvent
import io.github.qnicoud.dmgym.core.exercise.SlotDrawer
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.exercise.defaultDuration
import io.github.qnicoud.dmgym.core.exercise.durationMs
import io.github.qnicoud.dmgym.core.exercise.milestonesReached
import io.github.qnicoud.dmgym.core.exercise.pausedTimerFrom
import io.github.qnicoud.dmgym.core.exercise.progressOf
import io.github.qnicoud.dmgym.core.exercise.reduce
import io.github.qnicoud.dmgym.core.exercise.timerSnapshotOf
import io.github.qnicoud.dmgym.core.exercise.timerUi
import io.github.qnicoud.dmgym.core.exercise.toExerciseSession
import io.github.qnicoud.dmgym.core.exercise.toLongs
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.tables.NotEnoughEntriesException
import io.github.qnicoud.dmgym.core.tables.TableLoadException
import io.github.qnicoud.dmgym.core.timer.TickResult
import io.github.qnicoud.dmgym.core.timer.TimedEvent
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerEvent
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * ViewModel générique des exercices (ADR 0012). Il enchaîne tirage, minuteur, sauvegarde automatique et
 * reprise. Une instance de [Random] (donc un [SlotDrawer]) par ViewModel.
 */
class ExerciseViewModel(
    private val definition: ExerciseDefinition,
    private val deps: ExerciseDependencies,
    private val handle: SavedStateHandle,
    private val launch: ExerciseLaunch = ExerciseLaunch(),
    private val engine: TimerEngine = TimerEngine(),
) : ViewModel() {
    private val seed: Long = handle.get<Long>(KEY_SEED) ?: launch.seed.also { handle[KEY_SEED] = it }
    private val random = Random(seed)
    private val drawer = SlotDrawer(deps.tables, random)
    private val recorder = SessionRecorder(deps.history, deps.wallClock, deps.ioDispatcher)
    private val model = MutableStateFlow(ExerciseModel(ExerciseSession(definition.id), TimerState.Idle(DEFAULT_MS)))
    private val channel = Channel<ExerciseEvent>(Channel.BUFFERED)
    private val fields = mutableMapOf<AnswerKey, TextFieldState>()
    private var plan: EventPlan = EventPlan(engine, emptyList(), null, random)
    private var lastElapsedMs: Long? = null
    private var reached: Set<Milestone> = emptySet()
    private var starting = false
    private var signalDelivered = handle.get<Boolean>(KEY_SIGNAL) ?: false

    /** Événements ponctuels (annonces, rappels). */
    val events: Flow<ExerciseEvent> = channel.receiveAsFlow()

    /** Les tics ne tournent que tant que l'écran collecte : rien ne sonne en arrière-plan (ADR 0006). */
    val uiState: StateFlow<ExerciseUiState> =
        combine(model, ticks().onEach(::onTick)) { m, now -> m.toUiState(definition, engine, now) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                model.value.toUiState(definition, engine, 0),
            )

    init {
        deps.endSignal.prepare()
        viewModelScope.launch { initialise() }
        viewModelScope.launch {
            model.map { it.session }.autosave(AUTOSAVE_DEBOUNCE_MS, ::persist)
        }
    }

    // region intentions

    fun fieldState(key: AnswerKey): TextFieldState =
        fields.getOrPut(key) {
            TextFieldState(model.value.session.textOf(key)).also { state ->
                viewModelScope.launch { snapshotFlow { state.text.toString() }.collect { onAnswerChanged(key, it) } }
            }
        }

    fun onAnswerChanged(
        key: AnswerKey,
        text: String,
    ) = dispatch(ExerciseAction.AnswerChanged(key, text))

    fun onFlagToggled(key: AnswerKey) = dispatch(ExerciseAction.FlagToggled(key))

    fun onCheckToggled(key: DrawKey) = dispatch(ExerciseAction.CheckToggled(key))

    fun onOptionChanged(
        key: String,
        value: Boolean,
    ) = dispatch(ExerciseAction.OptionChanged(key, value))

    fun onNextQuestion() {
        val layout = definition.layout as? FieldLayout.Sequential ?: return
        dispatch(
            ExerciseAction.NextQuestion(
                model.value.session
                    .drawsOf(layout.questionsSlot)
                    .lastIndex,
            ),
        )
    }

    fun onAddGroup() {
        val layout = definition.layout as? FieldLayout.RepeatableGroup ?: return
        model.update { it.copy(extraGroups = (it.extraGroups + 1).coerceAtMost(layout.max - 1)) }
    }

    fun onAssess(
        rating: Int?,
        isGem: Boolean,
        rubric: Map<String, Boolean>,
    ) = dispatch(ExerciseAction.Assessed(rating, isGem, rubric))

    /** Relance un slot (ou tous) ; le chrono et le texte sont conservés. */
    fun onReroll(slotKey: String? = null) {
        if (model.value.session.phase == Phase.Ended) return
        viewModelScope.launch {
            val session = model.value.session
            val slots = definition.slots.filter { slotKey == null || it.key == slotKey }
            try {
                val draws = slots.flatMap { drawer.draw(it, session.drawsOf(it.key).map { d -> d.entryId }.toSet()) }
                dispatch(ExerciseAction.Rerolled(slotKey, draws))
            } catch (_: NotEnoughEntriesException) {
                model.update { it.copy(drawFailed = true) }
            } catch (_: TableLoadException) {
                model.update { it.copy(drawFailed = true) }
            }
        }
    }

    /** « Réessayer » de la carte d'erreur. */
    fun onRetry() {
        model.update { it.copy(loading = true, drawFailed = false) }
        viewModelScope.launch { drawFirst() }
    }

    /** « Commencer » : crée la session et lance le chrono. */
    fun onStart() {
        val m = model.value
        if (starting || m.loading || m.session.phase != Phase.Ready) return
        if (m.drawFailed && !definition.acceptsFreeSubject) return
        starting = true
        viewModelScope.launch {
            val planned = (m.timer as? TimerState.Idle)?.durationMs?.takeUnless { m.untimed }
            val id = recorder.create(definition.id.value, planned?.let { (it / MS).toInt() }, launch.fromDaily, seed)
            handle[KEY_SESSION] = id
            val now = deps.monotonicClock.now()
            val timer = if (planned == null) engine.startUntimed(now) else engine.start(planned, now)
            plan = EventPlan(engine, definition.events, planned, random)
            lastElapsedMs = 0
            updateTimer(timer)
            dispatch(ExerciseAction.Started(id))
            channel.trySend(ExerciseEvent.Announce(Milestone.Started))
        }
    }

    fun onPause() = changeTimer { engine.pause(it, deps.monotonicClock.now()) }

    fun onResume() = changeTimer { TickResult(engine.resume(it, deps.monotonicClock.now())) }

    /** « Terminer » : fige le chrono, enregistre COMPLETED, ouvre le bilan. */
    fun onFinish() {
        if (model.value.session.phase != Phase.Active) return
        changeTimer { engine.pause(it, deps.monotonicClock.now()) }
        recorder.markEnded()
        dispatch(ExerciseAction.Finished)
    }

    /** À appeler à l'arrêt de l'écran : écrit tout de suite, sans attendre le debounce. */
    fun onStop() {
        viewModelScope.launch { withContext(NonCancellable) { flush() } }
    }

    /** Sortie de l'écran : écrit le brouillon, ou supprime la session si elle n'a aucun contenu. */
    fun onLeave() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val session = model.value.session
                val id = session.sessionId
                if (id != null && !session.hasContent) recorder.delete(id) else flush()
            }
        }
    }

    override fun onCleared() = deps.endSignal.release()

    // endregion

    // region minuteur

    private fun ticks(): Flow<Long> =
        flow {
            while (true) {
                emit(deps.monotonicClock.now())
                delay(TICK_MS)
            }
        }

    private fun onTick(now: Long) {
        val m = model.value
        if (m.session.phase != Phase.Active) return
        val result = engine.tick(m.timer, now)
        if (result.state != m.timer) updateTimer(result.state)
        handleExpired(result.expired)
        fireDueEvents(result.state, now)
        announceMilestones(result.state, now)
    }

    private fun changeTimer(transform: (TimerState) -> TickResult) {
        val m = model.value
        if (m.session.phase != Phase.Active || (m.timer is TimerState.Idle)) return
        val result = transform(m.timer)
        updateTimer(result.state)
        handleExpired(result.expired)
    }

    private fun updateTimer(state: TimerState) {
        model.update { it.copy(timer = state) }
        val snapshot = engine.snapshot(state, deps.wallClock.millis(), deps.monotonicClock.now())
        handle[KEY_TIMER] = snapshot.toLongs()
    }

    private fun handleExpired(expired: TimerEvent.Expired?) {
        if (expired == null || !expired.fresh || signalDelivered) return
        signalDelivered = true
        handle[KEY_SIGNAL] = true
        viewModelScope.launch { deps.endSignal.play(deps.settings.settings.first()) }
        channel.trySend(ExerciseEvent.Announce(Milestone.TimeUp))
    }

    private fun announceMilestones(
        state: TimerState,
        now: Long,
    ) {
        val ui = timerUi(state, engine, now, untimedReady = false)
        val durationMs = state.durationMs() ?: return
        val fresh = milestonesReached(ui, durationMs) - reached
        reached = reached + fresh
        fresh.forEach { channel.trySend(ExerciseEvent.Announce(it)) }
    }

    private fun fireDueEvents(
        state: TimerState,
        now: Long,
    ) {
        if (state is TimerState.Paused || state is TimerState.OvertimePaused ||
            state is TimerState.UntimedPaused
        ) {
            return
        }
        val elapsed = engine.elapsedMs(state, now)
        val previous = lastElapsedMs
        lastElapsedMs = elapsed
        plan.due(previous, elapsed).forEach { fire(it) }
    }

    private fun fire(due: DueEvent) {
        val event = due.event
        when (val timing = event.timing) {
            is TimedEvent.Periodic -> if (!due.stale) channel.trySend(ExerciseEvent.Reminder(event.id, timing.haptic))
            is TimedEvent.Once -> if (event.id !in model.value.session.firedEvents) fireOnce(event)
        }
    }

    private fun fireOnce(event: ScheduledEvent) {
        viewModelScope.launch {
            val draws = event.draw?.let { runCatching { drawer.draw(it) }.getOrDefault(emptyList()) }.orEmpty()
            dispatch(ExerciseAction.EventFired(event.id, draws))
        }
    }

    // endregion

    // region chargement et reprise

    private suspend fun initialise() {
        val settings = deps.settings.settings.first()
        val untimed =
            definition.timer is TimerPolicy.None || definition.id in settings.untimed
        val duration: Duration =
            settings.durations[definition.id] ?: definition.timer.defaultDuration
                ?: ExerciseId.DEFAULT_DURATIONS[definition.id] ?: FALLBACK_DURATION
        model.update { it.copy(timer = TimerState.Idle(duration.inWholeMilliseconds), untimed = untimed) }
        val resumeId = handle.get<Long>(KEY_SESSION) ?: launch.resumeSessionId
        val detail = resumeId?.let { withContext(deps.ioDispatcher) { deps.history.observeDetail(it).first() } }
        if (detail != null && detail.session.exerciseId == definition.id.value) restore(detail) else drawFirst()
    }

    private suspend fun drawFirst() {
        try {
            val draws = drawer.drawAll(definition.slots)
            model.update { it.copy(session = reduce(it.session, ExerciseAction.Drawn(draws)), loading = false) }
        } catch (_: NotEnoughEntriesException) {
            model.update { it.copy(loading = false, drawFailed = true) }
        } catch (_: TableLoadException) {
            model.update { it.copy(loading = false, drawFailed = true) }
        }
    }

    private fun restore(detail: SessionDetail) {
        val session = detail.toExerciseSession()
        recorder.adopt(detail.session)
        val wall = deps.wallClock.millis()
        val now = deps.monotonicClock.now()
        val snapshot = timerSnapshotOf(handle.get<LongArray>(KEY_TIMER))
        val timer =
            if (snapshot != null) {
                engine.restore(snapshot, wall, now)
            } else {
                pausedTimerFrom(detail.session.plannedDurationSec, detail.session.activeDurationSec)
            }
        val untimed = timer is TimerState.Untimed || timer is TimerState.UntimedPaused
        val extra = (session.answers.keys.maxOfOrNull { it.groupIndex } ?: 0)
        model.value = ExerciseModel(session, timer, untimed, loading = false, extraGroups = extra)
        handle[KEY_SESSION] = detail.session.id
        signalDelivered = signalDelivered || timer is TimerState.Overtime || timer is TimerState.OvertimePaused
        lastElapsedMs = engine.elapsedMs(timer, now)
        plan = EventPlan(engine, definition.events, timer.durationMs(), random)
        reached = timer.durationMs()?.let { milestonesReached(timerUi(timer, engine, now, false), it) }.orEmpty()
        updateTimer(timer)
    }

    // endregion

    // region sauvegarde

    private fun dispatch(action: ExerciseAction) {
        model.update { it.copy(session = reduce(it.session, action)) }
    }

    private suspend fun flush() {
        model.value.session
            .takeIf { it.sessionId != null }
            ?.let { persist(it) }
    }

    private suspend fun persist(session: ExerciseSession) {
        val state = model.value.timer
        val now = deps.monotonicClock.now()
        val overtime = engine.overtimeMs(state, now)
        val times = SessionTimes(activeMs = engine.elapsedMs(state, now), overtimeMs = overtime)
        val outcome = runCatching { recorder.save(session, times) }
        model.update { it.copy(save = if (outcome.getOrDefault(false)) SaveStatus.Saved else SaveStatus.Failed) }
    }

    // endregion

    private companion object {
        const val KEY_SEED = "seed"
        const val KEY_SESSION = "session_id"
        const val KEY_TIMER = "timer"
        const val KEY_SIGNAL = "signal_delivered"
        const val TICK_MS = 250L
        const val STOP_TIMEOUT_MS = 5_000L
        const val AUTOSAVE_DEBOUNCE_MS = 1_000L
        const val MS = 1_000L
        const val DEFAULT_MS = 60_000L
        val FALLBACK_DURATION: Duration = 5.minutes
    }
}
