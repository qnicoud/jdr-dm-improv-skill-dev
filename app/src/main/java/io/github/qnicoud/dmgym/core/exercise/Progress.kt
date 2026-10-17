package io.github.qnicoud.dmgym.core.exercise

/** Progression « x/n » affichée à côté du minuteur. */
data class Progress(
    val done: Int,
    val total: Int,
)

/** Progression de [session] selon la règle de [definition], ou `null` si l'exercice n'en affiche pas. */
fun progressOf(
    definition: ExerciseDefinition,
    session: ExerciseSession,
): Progress? =
    when (definition.progress) {
        ProgressRule.None -> {
            null
        }

        ProgressRule.FilledFields -> {
            val keys = definition.fieldKeys
            Progress(done = keys.count { session.textOf(AnswerKey(it)).isNotBlank() }, total = keys.size)
        }
    }
