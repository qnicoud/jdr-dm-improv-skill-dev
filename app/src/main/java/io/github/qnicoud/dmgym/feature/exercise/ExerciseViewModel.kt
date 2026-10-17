package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.Milestone
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.SlotDrawer
import io.github.qnicoud.dmgym.core.exercise.TimerUi
import io.github.qnicoud.dmgym.core.exercise.durationMs
import io.github.qnicoud.dmgym.core.exercise.milestonesReached
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
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerEvent
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val scheduled =
        ScheduledEventRunner(engine, definition.events, random, drawer, viewModelScope, channel, ::dispatch) {
            model.value.session.firedEvents
        }
    private val fields = FieldStates(viewModelScope, { model.value.session.textOf(it) }, ::onAnswerChanged)
    private var reached: Set<Milestone> = emptySet()
    private var starting = false
    private var signalDelivered = handle.get<Boolean>(KEY_SIGNAL) ?: false

    /** Événements ponctuels (annonces, rappels). */
    val events: Flow<ExerciseEvent> = channel.receiveAsFlow()

    val uiState: StateFlow<ExerciseUiState> =
        model
            .map { it.toUiState(definition) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), model.value.toUiState(definition))

    /**
     * Minuteur, séparé de [uiState] pour qu'un tic toutes les 250 ms ne recompose que lui. Les tics ne tournent que
     * tant que l'écran le collecte : rien ne sonne en arrière-plan (ADR 0006).
     */
    val timerState: StateFlow<TimerUi> =
        combine(model, ticks().onEach(::onTick)) { m, now -> m.toTimerUi(engine, now) }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), model.value.toTimerUi(engine, 0))

    /** Vibration du rappel périodique : réglage dédié et vibration globale activés (D41). */
    val hapticReminders: StateFlow<Boolean> =
        deps.settings.settings
            .map { it.crazyEightsMinuteHaptic && it.vibrationEnabled }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        deps.endSignal.prepare()
        viewModelScope.launch { initialise() }
        viewModelScope.launch {
            model.map { it.session }.autosave(AUTOSAVE_DEBOUNCE_MS, ::persist)
        }
    }

    // region intentions

    fun fieldState(key: AnswerKey): TextFieldState = fields.get(key)

    fun onAnswerChanged(
        key: AnswerKey,
        text: String,
    ) {
        dispatch(ExerciseAction.AnswerChanged(key, text))
        if (key.fieldKey == definition.detection?.field) redetect()
    }

    /** Recalcule les cases « utilisé » ; le cochage manuel n'est jamais touché (réducteur, ADR 0008). */
    private fun redetect() {
        val spec = definition.detection ?: return
        val detector = deps.detector ?: return
        val session = model.value.session
        val draws = session.draws.filter { it.slotKey in spec.slotKeys }
        dispatch(ExerciseAction.AutoDetected(detector.detect(draws, session.textOf(AnswerKey(spec.field)))))
    }

    /** Charge les mots-clés des tirages courants puis recalcule les cases. À rappeler après chaque tirage. */
    private suspend fun prepareDetection() {
        val spec = definition.detection ?: return
        val detector = deps.detector ?: return
        detector.prepare(
            model.value.session.draws
                .filter { it.slotKey in spec.slotKeys },
        )
        if (model.value.session
                .textOf(AnswerKey(spec.field))
                .isNotBlank()
        ) {
            redetect()
        }
    }

    /** Plages du [text] à surligner (champ surveillé) ; synchrone, appelé à chaque rendu du champ. */
    fun highlights(text: String): List<IntRange> {
        val spec = definition.detection ?: return emptyList()
        val detector = deps.detector ?: return emptyList()
        return detector.highlights(
            model.value.session.draws
                .filter { it.slotKey in spec.slotKeys },
            text,
        )
    }

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
        handle[KEY_QUESTION] = model.value.session.currentQuestion
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

    /** Relance un slot (ou tous) ; le chrono et le texte sont conservés, et un échec garde les tirages actuels. */
    fun onReroll(slotKey: String? = null) {
        if (model.value.session.phase == Phase.Ended) return
        viewModelScope.launch {
            val session = model.value.session
            val slots = definition.slots.filter { slotKey == null || it.key == slotKey }
            try {
                val draws = slots.flatMap { drawer.draw(it, session.drawsOf(it.key).map { d -> d.entryId }.toSet()) }
                dispatch(ExerciseAction.Rerolled(slotKey, draws))
                prepareDetection()
            } catch (_: NotEnoughEntriesException) {
                channel.trySend(ExerciseEvent.Snackbar(R.string.draw_reroll_failed))
            } catch (_: TableLoadException) {
                channel.trySend(ExerciseEvent.Snackbar(R.string.draw_reroll_failed))
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
        val now = deps.monotonicClock.now()
        viewModelScope.launch {
            val planned = (m.timer as? TimerState.Idle)?.durationMs?.takeUnless { m.untimed }
            val id = createSession(planned) ?: return@launch
            handle[KEY_SESSION] = id
            val timer = if (planned == null) engine.startUntimed(now) else engine.start(planned, now)
            scheduled.reset(planned, startElapsedMs = 0)
            updateTimer(timer)
            dispatch(ExerciseAction.Started(id))
            channel.trySend(ExerciseEvent.Announce(Milestone.Started))
        }
    }

    private suspend fun createSession(plannedMs: Long?): Long? =
        try {
            recorder.create(definition.id.value, plannedMs?.let { (it / MS).toInt() }, launch.fromDaily, seed)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            starting = false
            channel.trySend(ExerciseEvent.Snackbar(R.string.start_failed))
            null
        }

    fun onPause() = changeTimer { engine.pause(it, deps.monotonicClock.now()) }

    fun onResume() = changeTimer { TickResult(engine.resume(it, deps.monotonicClock.now())) }

    /** « Terminer » : fige le chrono, enregistre COMPLETED, ouvre le bilan. */
    fun onFinish() {
        if (model.value.session.phase != Phase.Active) return
        // Terminer n'est pas une échéance : l'éventuel signal de fin est volontairement ignoré.
        updateTimer(engine.pause(model.value.timer, deps.monotonicClock.now()).state)
        recorder.markEnded()
        dispatch(ExerciseAction.Finished)
    }

    /** À appeler à l'arrêt de l'écran : écrit tout de suite, sans attendre le debounce. */
    fun onStop() {
        fields.syncNow()
        viewModelScope.launch { withContext(NonCancellable) { flush() } }
    }

    /** Sortie de l'écran : écrit le brouillon, ou supprime la session si elle n'a aucun contenu. */
    fun onLeave() {
        fields.syncNow()
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
        scheduled.onTick(result.state, now)
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

    // endregion

    // region chargement et reprise

    private suspend fun initialise() {
        val settings = deps.settings.settings.first()
        val (durationMs, untimed) = timerConfig(definition, settings)
        model.update { it.copy(timer = TimerState.Idle(durationMs), untimed = untimed) }
        val resumeId = handle.get<Long>(KEY_SESSION) ?: launch.resumeSessionId
        val detail = resumeId?.let { withContext(deps.ioDispatcher) { deps.history.observeDetail(it).first() } }
        if (detail != null && detail.session.exerciseId == definition.id.value) {
            restore(detail)
            prepareDetection()
        } else {
            drawFirst()
        }
    }

    private suspend fun drawFirst() {
        try {
            val draws = drawer.drawAll(definition.slots)
            model.update { it.copy(session = reduce(it.session, ExerciseAction.Drawn(draws)), loading = false) }
            prepareDetection()
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
        val timer = snapshot?.let { engine.restore(it, wall, now) } ?: pausedTimerFor(detail.session)
        val untimed = timer is TimerState.Untimed || timer is TimerState.UntimedPaused
        val extra = (session.answers.keys.maxOfOrNull { it.groupIndex } ?: 0)
        val question = handle.get<Int>(KEY_QUESTION) ?: 0
        model.value =
            ExerciseModel(
                session.copy(currentQuestion = question),
                timer,
                untimed,
                loading = false,
                extraGroups = extra,
            )
        handle[KEY_SESSION] = detail.session.id
        signalDelivered = signalDelivered || timer is TimerState.Overtime || timer is TimerState.OvertimePaused
        scheduled.reset(timer.durationMs(), engine.elapsedMs(timer, now))
        reached = timer.durationMs()?.let { milestonesReached(timerUi(timer, engine, now, false), it) }.orEmpty()
        updateTimer(timer)
    }

    // endregion

    // region sauvegarde

    private fun dispatch(action: ExerciseAction) {
        model.update { it.copy(session = reduce(it.session, action)) }
    }

    private suspend fun flush() {
        fields.syncNow()
        model.value.session
            .takeIf { it.sessionId != null }
            ?.let { persist(it) }
    }

    private suspend fun persist(session: ExerciseSession) {
        val state = model.value.timer
        val now = deps.monotonicClock.now()
        val overtime = engine.overtimeMs(state, now)
        val times = SessionTimes(activeMs = engine.elapsedMs(state, now), overtimeMs = overtime)
        val saved =
            try {
                recorder.save(session, times)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        model.update { it.copy(save = if (saved) SaveStatus.Saved else SaveStatus.Failed) }
    }

    // endregion

    private companion object {
        const val KEY_SEED = "seed"
        const val KEY_SESSION = "session_id"
        const val KEY_TIMER = "timer"
        const val KEY_SIGNAL = "signal_delivered"
        const val KEY_QUESTION = "question"
        const val TICK_MS = 250L
        const val STOP_TIMEOUT_MS = 5_000L
        const val AUTOSAVE_DEBOUNCE_MS = 1_000L
        const val MS = 1_000L
        const val DEFAULT_MS = 60_000L
    }
}
