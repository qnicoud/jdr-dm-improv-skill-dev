package io.github.qnicoud.dmgym.core.exercise

/**
 * Choix facultatif du bilan « Laquelle aurais-tu jouée en premier ? » (Decision Point, recette rc1, critère 1.11).
 * Il est enregistré dans la grille d'auto-évaluation, sans changement de schéma : une entrée
 * `first_pick:<clé du champ> = true`. Au plus une entrée existe à la fois.
 */
object FirstPick {
    const val PREFIX = "first_pick:"

    fun key(fieldKey: String): String = PREFIX + fieldKey

    fun isKey(rubricKey: String): Boolean = rubricKey.startsWith(PREFIX)

    /** Clé du champ choisi, ou `null`. */
    fun chosen(rubric: Map<String, Boolean>): String? =
        rubric.entries
            .firstOrNull { isKey(it.key) && it.value }
            ?.key
            ?.removePrefix(PREFIX)

    /** [rubric] avec [fieldKey] pour seul choix, ou sans choix si `null`. */
    fun with(
        rubric: Map<String, Boolean>,
        fieldKey: String?,
    ): Map<String, Boolean> {
        val others = rubric.filterKeys { !isKey(it) }
        return if (fieldKey == null) others else others + (key(fieldKey) to true)
    }

    /** [rubric] sans les choix qui pointent vers un champ vide ou absent de [answers] (clé orpheline). */
    fun pruned(
        rubric: Map<String, Boolean>,
        answers: Map<AnswerKey, Answer>,
    ): Map<String, Boolean> {
        val filled =
            answers
                .filterValues { it.text.isNotBlank() }
                .keys
                .map { it.fieldKey }
                .toSet()
        return rubric.filterKeys { !isKey(it) || it.removePrefix(PREFIX) in filled }
    }
}

/** Clés des champs proposés au choix du bilan ; vide si l'exercice n'a pas ce choix. */
fun ExerciseDefinition.firstPickFields(): List<String> =
    if (endChoice) (layout as? FieldLayout.Fixed)?.fields.orEmpty().map { it.key } else emptyList()
