package io.github.qnicoud.dmgym.feature.exercise

import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.Milestone
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.withCategories
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Vie d'une séance : démarrage et création de la session, fin, écriture à l'arrêt de l'écran, sortie avec
 * suppression d'une session vide, reprise d'une session enregistrée.
 */
internal class SessionLifecycle(
    private val definition: ExerciseDefinition,
    private val deps: ExerciseDependencies,
    private val handle: SavedStateHandle,
    private val launch: ExerciseLaunch,
    private val seed: Long,
    private val model: MutableStateFlow<ExerciseModel>,
    private val recorder: SessionRecorder,
    private val fields: FieldStates,
    private val ready: ReadyController,
    private val timer: TimerController,
    private val scope: CoroutineScope,
    private val channel: Channel<ExerciseEvent>,
    private val dispatch: (ExerciseAction) -> Unit,
) {
    private var starting = false

    /** « Commencer » : crée la session et lance le chrono. */
    fun start() {
        val m = model.value
        if (starting || m.loading || m.session.phase != Phase.Ready) return
        ready.flush()
        if (m.drawFailed && !definition.acceptsFreeSubject) return
        starting = true
        val now = deps.monotonicClock.now()
        scope.launch {
            val planned = (m.timer as? TimerState.Idle)?.durationMs?.takeUnless { m.untimed }
            val id = createSession(planned) ?: return@launch
            handle[HandleKeys.SESSION] = id
            timer.start(planned, now)
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

    /** « Terminer » : fige le chrono, enregistre COMPLETED, ouvre le bilan. */
    fun finish() {
        if (model.value.session.phase != Phase.Active) return
        timer.freeze()
        recorder.markEnded()
        dispatch(ExerciseAction.Finished)
    }

    /** À l'arrêt de l'écran : écrit tout de suite, sans attendre le debounce. */
    fun stop() {
        ready.flush()
        fields.syncNow()
        scope.launch { withContext(NonCancellable) { flush() } }
    }

    /** Sortie de l'écran : écrit le brouillon, ou supprime la session si elle n'a aucun contenu. */
    fun leave() {
        fields.syncNow()
        scope.launch {
            withContext(NonCancellable) {
                val session = model.value.session
                val id = session.sessionId
                if (id != null && !session.hasContent && !definition.isPlayedWithoutText(session)) {
                    recorder.delete(id)
                } else {
                    flush()
                }
            }
        }
    }

    /** Remet l'écran dans l'état de la session enregistrée [detail]. */
    suspend fun restore(
        detail: SessionDetail,
        restorer: SessionRestorer,
    ) {
        recorder.adopt(detail.session)
        val base = restorer.restored(detail)
        val draws = base.session.draws.withCategories(deps.tables)
        val restored = base.copy(session = base.session.copy(draws = draws))
        model.value = restored
        handle[HandleKeys.SESSION] = detail.session.id
        timer.restore(restored.timer)
    }

    suspend fun flush() {
        fields.syncNow()
        model.value.session
            .takeIf { it.sessionId != null }
            ?.let { persist(it) }
    }

    suspend fun persist(session: ExerciseSession) {
        val saved =
            try {
                recorder.save(session, timer.times())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        model.update { it.copy(save = if (saved) SaveStatus.Saved else SaveStatus.Failed) }
    }

    private companion object {
        const val MS = 1_000L
    }
}
