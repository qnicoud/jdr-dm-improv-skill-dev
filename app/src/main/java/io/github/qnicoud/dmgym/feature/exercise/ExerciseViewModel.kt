package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.Draw
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
        ScheduledEventRunner(
            engine,
            definition.events,
            random,
            drawer,
            viewModelScope,
            channel,
            ::dispatch,
            options = { definition.resolveOptions(model.value.session.options) },
            firedEvents = { model.value.session.firedEvents },
        )
    private val restorer = SessionRestorer(definition, deps, handle, launch, engine)
    private val drawing = DrawCoordinator(definition, drawer, channel, ::dispatch) { model.value.session }
    private val fields = FieldStates(viewModelScope, { model.value.session.textOf(it) }, ::onAnswerChanged)
    private var starting = false
    private val announcer = TimerAnnouncer(engine, deps, handle, viewModelScope, channel)

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
        combine(model, ticksOf(deps.monotonicClock, TICK_MS).onEach(::onTick)) { m, now -> m.toTimerUi(engine, now) }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), model.value.toTimerUi(engine, 0))

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
        val isNote = key.fieldKey == definition.endNote?.key
        dispatch(if (isNote) ExerciseAction.NoteChanged(key, text) else ExerciseAction.AnswerChanged(key, text))
        if (key.fieldKey == definition.detection?.field) redetect()
    }

    /** Recalcule les cases « utilisé » ; le changement suit le debounce de la frappe (voir `autosave`). */
    private fun redetect() {
        val spec = definition.detection ?: return
        val detector = deps.detector ?: return
        val session = model.value.session
        val draws = session.draws.filter { it.slotKey in spec.slotKeys }
        dispatch(ExerciseAction.AutoDetected(detector.detect(draws, session.textOf(AnswerKey(spec.field)))))
    }

    fun onFlagToggled(key: AnswerKey) = dispatch(ExerciseAction.FlagToggled(key))

    fun onCheckToggled(key: DrawKey) = dispatch(ExerciseAction.CheckToggled(key))

    /** Change une option ; avant le départ, les tirages qui en dépendent (mode, contre-stéréotype) sont refaits. */
    fun onOptionChanged(
        key: String,
        value: Boolean,
    ) {
        dispatch(ExerciseAction.OptionChanged(key, value))
        if (model.value.session.phase == Phase.Ready) viewModelScope.launch { drawing.redrawForOption(key) }
    }

    fun onNextQuestion() = moveQuestion { ExerciseAction.NextQuestion(it.lastIndex) }

    fun onPreviousQuestion() = moveQuestion { ExerciseAction.PreviousQuestion }

    private fun moveQuestion(action: (List<Draw>) -> ExerciseAction) {
        val layout = definition.layout as? FieldLayout.Sequential ?: return
        dispatch(action(model.value.session.drawsOf(layout.questionsSlot)))
        handle[HandleKeys.QUESTION] = model.value.session.currentQuestion
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
        viewModelScope.launch { if (drawing.reroll(slotKey)) redetect() }
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
            handle[HandleKeys.SESSION] = id
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
                if (id != null && !session.hasContent &&
                    !definition.isPlayedWithoutText(session)
                ) {
                    recorder.delete(id)
                } else {
                    flush()
                }
            }
        }
    }

    override fun onCleared() = deps.endSignal.release()

    // endregion

    // region minuteur

    private fun onTick(now: Long) {
        val m = model.value
        if (m.session.phase != Phase.Active) return
        val result = engine.tick(m.timer, now)
        if (result.state != m.timer) updateTimer(result.state)
        announcer.onExpired(result.expired)
        scheduled.onTick(result.state, now)
        announcer.announce(result.state, now)
    }

    private fun changeTimer(transform: (TimerState) -> TickResult) {
        val m = model.value
        if (m.session.phase != Phase.Active || (m.timer is TimerState.Idle)) return
        val result = transform(m.timer)
        updateTimer(result.state)
        announcer.onExpired(result.expired)
    }

    private fun updateTimer(state: TimerState) {
        model.update { it.copy(timer = state) }
        val snapshot = engine.snapshot(state, deps.wallClock.millis(), deps.monotonicClock.now())
        handle[HandleKeys.TIMER] = snapshot.toLongs()
    }

    // endregion

    // region chargement et reprise

    private suspend fun initialise() {
        val start = restorer.start()
        model.update { start.model }
        val detail = start.detail
        if (detail == null) drawFirst() else restore(detail)
    }

    private suspend fun drawFirst() {
        val first = drawing.first()
        model.update {
            if (first ==
                null
            ) {
                it.copy(loading = false, drawFailed = true)
            } else {
                it.copy(session = reduce(it.session, first), loading = false)
            }
        }
    }

    private fun restore(detail: SessionDetail) {
        recorder.adopt(detail.session)
        val restored = restorer.restored(detail)
        val timer = restored.timer
        val now = deps.monotonicClock.now()
        model.value = restored
        handle[HandleKeys.SESSION] = detail.session.id
        scheduled.reset(timer.durationMs(), engine.elapsedMs(timer, now))
        announcer.resumeAt(timer, now)
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
        const val TICK_MS = 250L
        const val STOP_TIMEOUT_MS = 5_000L
        const val AUTOSAVE_DEBOUNCE_MS = 1_000L
        const val MS = 1_000L
        const val DEFAULT_MS = 60_000L
    }
}
