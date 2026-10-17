package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.SlotDrawer
import io.github.qnicoud.dmgym.core.exercise.activeFor
import io.github.qnicoud.dmgym.core.exercise.dependsOn
import io.github.qnicoud.dmgym.core.tables.NotEnoughEntriesException
import io.github.qnicoud.dmgym.core.tables.TableLoadException
import kotlinx.coroutines.channels.Channel

/**
 * Relances et tirages dépendant des options (mode vidéo ou table, contre-stéréotype). Un échec garde les tirages
 * actuels et prévient par un message bref.
 */
internal class DrawCoordinator(
    private val definition: ExerciseDefinition,
    private val drawer: SlotDrawer,
    private val out: Channel<ExerciseEvent>,
    private val dispatch: (ExerciseAction) -> Unit,
    private val session: () -> ExerciseSession,
) {
    /** Tirage initial ; `null` si une table est illisible ou trop pauvre (l'écran propose alors de réessayer). */
    suspend fun first(): ExerciseAction.Drawn? =
        try {
            ExerciseAction.Drawn(drawer.drawAll(definition.slots, definition.resolveOptions(session().options)))
        } catch (_: NotEnoughEntriesException) {
            null
        } catch (_: TableLoadException) {
            null
        }

    /** Relance un slot (ou tous les slots actifs) ; `true` si les tirages ont changé. */
    suspend fun reroll(slotKey: String?): Boolean {
        val current = session()
        val options = definition.resolveOptions(current.options)
        val slots = definition.slots.activeFor(options).filter { slotKey == null || it.key == slotKey }
        val exclude = slots.associate { it.key to current.drawsOf(it.key).map { d -> d.entryId }.toSet() }
        return attempt {
            dispatch(ExerciseAction.Rerolled(slotKey, drawer.redraw(slots, current.draws, options, exclude)))
        }
    }

    /** Refait avant le départ les tirages qui dépendent de [option] ; `true` s'il y en avait. */
    suspend fun redrawForOption(option: String): Boolean {
        val current = session()
        val options = definition.resolveOptions(current.options)
        val affected = definition.slots.filter { it.dependsOn(option) }
        if (affected.isEmpty()) return false
        return attempt {
            val fresh = drawer.redraw(affected.activeFor(options), current.draws, options)
            val kept = current.draws.filter { d -> affected.none { it.key == d.slotKey } }
            val order = definition.slots.map { it.key }
            dispatch(ExerciseAction.Drawn((kept + fresh).sortedBy { order.indexOf(it.slotKey) }))
        }
    }

    private suspend fun attempt(block: suspend () -> Unit): Boolean =
        try {
            block()
            true
        } catch (_: NotEnoughEntriesException) {
            out.trySend(ExerciseEvent.Snackbar(R.string.draw_reroll_failed))
            false
        } catch (_: TableLoadException) {
            out.trySend(ExerciseEvent.Snackbar(R.string.draw_reroll_failed))
            false
        }
}
