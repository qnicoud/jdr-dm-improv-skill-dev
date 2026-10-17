package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.SlotDrawer
import io.github.qnicoud.dmgym.core.exercise.TimerUi
import io.github.qnicoud.dmgym.core.exercise.reduce
import io.github.qnicoud.dmgym.core.exercise.toExerciseSession
import io.github.qnicoud.dmgym.core.settings.VibrationStrength
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel générique des exercices (ADR 0012) : une façade qui enchaîne tirage, minuteur, sauvegarde automatique et
 * reprise. Le travail est délégué à [TimerController], [SessionLifecycle] et [ReadyController]. Une instance de
 * [Random] (donc un [SlotDrawer]) par ViewModel.
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
            draws = { model.value.session.draws },
        )
    private val restorer = SessionRestorer(definition, deps, handle, launch, engine)
    private val drawing = DrawCoordinator(definition, drawer, channel, ::dispatch) { model.value.session }
    private val detection = DetectionCoordinator(definition, deps.detector, ::dispatch) { model.value.session }
    private val fields = FieldStates(viewModelScope, { model.value.session.textOf(it) }, ::onAnswerChanged)
    private val timer =
        TimerController(
            engine,
            deps,
            handle,
            model,
            scheduled,
            TimerAnnouncer(engine, deps, handle, viewModelScope, channel),
        )
    private val ready =
        ReadyController(definition, model, readySettingsWriter(), drawing, detection, viewModelScope, ::dispatch)
    private val lifecycle =
        SessionLifecycle(
            definition,
            deps,
            handle,
            launch,
            seed,
            model,
            SessionRecorder(deps.history, deps.wallClock, deps.ioDispatcher),
            fields,
            ready,
            timer,
            viewModelScope,
            channel,
            ::dispatch,
        )

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
        combine(
            model,
            ticksOf(deps.monotonicClock, TICK_MS).onEach(timer::onTick),
        ) { m, now -> m.toTimerUi(engine, now) }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), model.value.toTimerUi(engine, 0))

    /** Vibration du rappel périodique : réglage dédié et vibration globale activés (D41). */
    val hapticReminders: StateFlow<Boolean> =
        deps.settings.settings
            .map { it.crazyEightsMinuteHaptic && it.vibrationEnabled }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val vibrationStrength: StateFlow<VibrationStrength> =
        deps.settings.settings
            .map { it.vibrationStrength }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, VibrationStrength.NORMAL)

    /** Impulsion du rappel périodique, à la force choisie dans les réglages. */
    fun playReminderTick() = deps.endSignal.tick(vibrationStrength.value)

    init {
        deps.endSignal.prepare()
        viewModelScope.launch { initialise() }
        viewModelScope.launch {
            model.map { it.session }.autosave(AUTOSAVE_DEBOUNCE_MS, lifecycle::persist)
        }
    }

    private fun readySettingsWriter() =
        ReadySettingsWriter(definition.id, deps.settings, viewModelScope, deps.ioDispatcher) {
            val idle = model.value.timer as? TimerState.Idle
            ReadySetting((idle?.durationMs ?: DEFAULT_MS).milliseconds, model.value.untimed)
        }

    // region intentions

    fun fieldState(key: AnswerKey): TextFieldState = fields.get(key)

    fun onAnswerChanged(
        key: AnswerKey,
        text: String,
    ) {
        val isNote = key.fieldKey == definition.endNote?.key
        dispatch(if (isNote) ExerciseAction.NoteChanged(key, text) else ExerciseAction.AnswerChanged(key, text))
        if (key.fieldKey == definition.detection?.field) detection.redetect()
    }

    /** Plages du [text] à surligner (champ surveillé) ; synchrone, appelé à chaque rendu du champ. */
    fun highlights(text: String): List<IntRange> = detection.highlights(text)

    fun onFlagToggled(key: AnswerKey) = dispatch(ExerciseAction.FlagToggled(key))

    fun onCheckToggled(key: DrawKey) = dispatch(ExerciseAction.CheckToggled(key))

    /** Change une option ; avant le départ, les tirages qui en dépendent (mode, contre-stéréotype) sont refaits. */
    fun onOptionChanged(
        key: String,
        value: Boolean,
    ) = ready.changeOption(key, value)

    /** Écran Prêt : allonge d'un pas la durée de cet exercice (bornes de `DurationBounds`). */
    fun onDurationIncreased() = ready.increaseDuration()

    /** Écran Prêt : raccourcit d'un pas la durée de cet exercice. */
    fun onDurationDecreased() = ready.decreaseDuration()

    /** Écran Prêt : « sans minuteur » pour cet exercice, appliqué tout de suite et enregistré dans les Réglages. */
    fun onUntimedChanged(untimed: Boolean) = ready.changeUntimed(untimed)

    /** Fermeture de la feuille de durée : écrit sans attendre le regroupement des appuis. */
    fun onDurationSheetClosed() = ready.flush()

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
        viewModelScope.launch { if (drawing.reroll(slotKey)) detection.prepare() }
    }

    /** « Réessayer » de la carte d'erreur. */
    fun onRetry() {
        model.update { it.copy(loading = true, drawFailed = false) }
        viewModelScope.launch { drawFirst() }
    }

    /** « Commencer » : crée la session et lance le chrono. */
    fun onStart() = lifecycle.start()

    fun onPause() = timer.pause()

    fun onResume() = timer.resume()

    /** « Terminer » : fige le chrono, enregistre COMPLETED, ouvre le bilan. */
    fun onFinish() = lifecycle.finish()

    /** À appeler à l'arrêt de l'écran : écrit tout de suite, sans attendre le debounce. */
    fun onStop() = lifecycle.stop()

    /** Sortie de l'écran : écrit le brouillon, ou supprime la session si elle n'a aucun contenu. */
    fun onLeave() = lifecycle.leave()

    override fun onCleared() {
        ready.flushOnClear()
        deps.endSignal.release()
    }

    // endregion

    // region chargement et reprise

    private suspend fun initialise() {
        val start = restorer.start()
        model.update { start.model }
        val detail = start.detail
        if (detail == null) {
            drawFirst()
        } else {
            detection.loadKeywords(detail.toExerciseSession().draws)
            lifecycle.restore(detail, restorer)
            detection.redetectIfTyped()
        }
    }

    private suspend fun drawFirst() {
        val first = drawing.first()
        if (first != null) detection.loadKeywords(first.draws)
        model.update {
            if (first == null) {
                it.copy(loading = false, drawFailed = true)
            } else {
                it.copy(session = reduce(it.session, first), loading = false)
            }
        }
    }

    private fun dispatch(action: ExerciseAction) {
        model.update { it.copy(session = reduce(it.session, action)) }
    }

    // endregion

    private companion object {
        const val KEY_SEED = "seed"
        const val TICK_MS = 250L
        const val STOP_TIMEOUT_MS = 5_000L
        const val AUTOSAVE_DEBOUNCE_MS = 1_000L
        const val DEFAULT_MS = 60_000L
    }
}
