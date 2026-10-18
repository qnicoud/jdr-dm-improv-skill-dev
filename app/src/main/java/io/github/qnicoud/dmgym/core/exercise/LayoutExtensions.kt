package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.tables.TableFile
import io.github.qnicoud.dmgym.core.timer.TimedEvent

/**
 * Champs d'une question visibles pour les [options] de la session (un champ masqué n'est ni affiché ni compté).
 * [options] doit être résolu avec les valeurs par défaut de la définition (`ExerciseDefinition.resolveOptions`).
 */
fun FieldLayout.Sequential.visibleFields(options: Map<String, Boolean>): List<FieldSpec> =
    fields.filter { it.visibleWhen?.holds(options) ?: true }

/** Codes de [FieldLayout.Fixed.hintByCode] absents de [table] : une indication qui ne serait jamais affichée. */
fun FieldLayout.Fixed.unknownHintCodes(table: TableFile): Set<String> =
    hintByCode.keys - table.entries.map { it.id }.toSet()

/** Ce que le tirage lié à un champ ([FieldLayout.Fixed.boundSlot]) impose à ce champ. */
data class BoundField(
    /** Code stable de l'entrée tirée (son `id`) : il choisit l'icône et l'indication. */
    val code: String,
    /** Nom de l'entrée tirée, affiché dans le libellé (« Complication 1 · Coût »). */
    val name: String,
    /** Indication propre au code, ou `null` : celle du champ s'applique. */
    val hint: StringResId?,
)

/** Pour chaque champ, le tirage qui le contraint ; `null` sans lien ou si le tirage manque (sujet libre, relance). */
fun FieldLayout.Fixed.boundFields(draws: List<Draw>): List<BoundField?> {
    val bound = boundSlot?.let { slot -> draws.filter { it.slotKey == slot }.sortedBy { it.position } }.orEmpty()
    return fields.indices.map { i ->
        bound.getOrNull(i)?.let { BoundField(it.entryId, it.text, hintByCode[it.entryId]) }
    }
}

/**
 * Questions que l'écran peut montrer. Sans [FieldLayout.Sequential.revealProgressively], toutes. Avec : aucune avant
 * le départ, la seule question courante pendant le jeu, toutes une fois l'exercice terminé (bilan, historique).
 */
fun FieldLayout.Sequential.revealedQuestions(session: ExerciseSession): List<Draw> {
    val all = session.drawsOf(questionsSlot)
    if (!revealProgressively) return all
    return when (session.phase) {
        Phase.Ready -> {
            emptyList()
        }

        Phase.Active -> {
            listOfNotNull(
                all.getOrNull(session.currentQuestion.coerceIn(0, (all.size - 1).coerceAtLeast(0))),
            )
        }

        Phase.Ended -> {
            all
        }
    }
}

/** Avancement de la minute (ou de la période) en cours, entre 0 inclus et 1 exclu. Ne déclenche jamais rien. */
fun minuteFraction(
    elapsedMs: Long,
    periodMs: Long,
): Float = (elapsedMs.coerceAtLeast(0) % periodMs).toFloat() / periodMs

/**
 * Période du repère de rythme pour une durée de minuteur de [durationMs] : l'intervalle d'un rappel périodique, ou
 * la durée divisée par le nombre d'étapes. `null` sans rappel de rythme ou sans minuteur.
 */
fun ExerciseDefinition.rhythmPeriodMs(durationMs: Long?): Long? =
    events.map { it.timing }.firstNotNullOfOrNull { timing ->
        when (timing) {
            is TimedEvent.Periodic -> timing.intervalMs
            is TimedEvent.Steps -> durationMs?.takeIf { it > 0 }?.let { it / timing.count }
            is TimedEvent.Once -> null
        }
    }

/** Vrai si l'exercice a un repère de rythme (rappel périodique ou par étape). */
val ExerciseDefinition.hasRhythm: Boolean
    get() = events.any { it.timing is TimedEvent.Periodic || it.timing is TimedEvent.Steps }

/** Nombre de questions atteintes : jusqu'à la plus haute question répondue (au moins la première), dans [total]. */
fun reachedQuestions(
    answeredGroups: Collection<Int>,
    total: Int,
): Int = ((answeredGroups.maxOrNull() ?: 0) + 1).coerceIn(1, total.coerceAtLeast(1))

/** Indice de la dernière question répondue (0 s'il n'y en a aucune), où l'on reprend une session de l'historique. */
fun lastAnsweredQuestion(
    answeredGroups: Collection<Int>,
    total: Int,
): Int = reachedQuestions(answeredGroups, total) - 1
