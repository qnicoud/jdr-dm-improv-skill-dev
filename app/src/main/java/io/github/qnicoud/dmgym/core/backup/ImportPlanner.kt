package io.github.qnicoud.dmgym.core.backup

import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionIdentity
import io.github.qnicoud.dmgym.core.history.identity

/** Ce que l'import va faire, affiché dans l'aperçu avant confirmation (ADR 0015 §3.5). */
data class ImportPlan(
    val toInsert: List<SessionDetail>,
    val alreadyPresent: Int,
    val unknownExercise: Int,
    /** Sessions écartées car hors bornes. */
    val invalid: Int,
    /** Plage des jours locaux des sessions trouvées (ajoutées ou déjà présentes) ; `null` s'il n'y en a aucune. */
    val firstEpochDay: Long?,
    val lastEpochDay: Long?,
) {
    /** « 42 sessions trouvées » : celles qui seront ajoutées plus celles qui sont déjà là. */
    val found: Int get() = toInsert.size + alreadyPresent

    /** Sessions que le fichier contient mais qui ne seront pas importées : exercice inconnu ou hors bornes. */
    val ignored: Int get() = unknownExercise + invalid

    val hasSomethingToAdd: Boolean get() = toInsert.isNotEmpty()
}

object ImportPlanner {
    /** La session déjà présente gagne toujours (D66) ; un doublon à l'intérieur du fichier compte une seule fois. */
    fun plan(
        sessions: List<SessionDetail>,
        unknownExercise: Int,
        existing: Set<SessionIdentity>,
        invalid: Int = 0,
    ): ImportPlan {
        val unique = sessions.distinctBy { it.session.identity }
        val (present, fresh) = unique.partition { it.session.identity in existing }
        val days = unique.map { it.session.localEpochDay }
        return ImportPlan(
            toInsert = fresh,
            alreadyPresent = present.size,
            unknownExercise = unknownExercise,
            invalid = invalid,
            firstEpochDay = days.minOrNull(),
            lastEpochDay = days.maxOrNull(),
        )
    }
}
