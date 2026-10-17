package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/** Réglage de l'écran Prêt tel qu'il doit être écrit : durée en cours et mode « sans minuteur ». */
internal data class ReadySetting(
    val duration: Duration,
    val untimed: Boolean,
)

/**
 * Écrit dans les Réglages la durée choisie sur l'écran Prêt. Les appuis rapprochés sur − / + sont regroupés
 * (une seule écriture, après [DEBOUNCE_MS]) ; [flush] écrit tout de suite (fermeture de la feuille, départ) et
 * [flushOnClear] le fait aussi quand le ViewModel disparaît.
 */
internal class ReadySettingsWriter(
    private val id: ExerciseId,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val current: () -> ReadySetting,
) {
    private var job: Job? = null
    private var durationDirty = false
    private var untimedDirty = false

    fun durationChanged() {
        durationDirty = true
        schedule()
    }

    fun untimedChanged() {
        untimedDirty = true
        schedule()
    }

    private fun schedule() {
        job?.cancel()
        job =
            scope.launch {
                delay(DEBOUNCE_MS)
                write()
            }
    }

    fun flush() {
        job?.cancel()
        if (durationDirty || untimedDirty) scope.launch { write() }
    }

    /** Le scope du ViewModel est déjà annulé : l'écriture en attente part sur un scope à elle. */
    fun flushOnClear() {
        job?.cancel()
        if (durationDirty || untimedDirty) CoroutineScope(SupervisorJob() + ioDispatcher).launch { write() }
    }

    private suspend fun write() {
        val setting = current()
        val duration = durationDirty
        val untimed = untimedDirty
        durationDirty = false
        untimedDirty = false
        settings.update { stored ->
            var next = stored
            if (duration) next = next.copy(durations = next.durations + (id to setting.duration))
            if (untimed) {
                next = next.copy(untimed = if (setting.untimed) next.untimed + id else next.untimed - id)
            }
            next
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
    }
}
