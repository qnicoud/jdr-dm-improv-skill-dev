package io.github.qnicoud.dmgym.feature.settings

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.StringResId
import io.github.qnicoud.dmgym.core.exercise.TableSlot

/**
 * Libellés lisibles de l'export : nom d'exercice, champ, question de la grille et slot de tirage, par exercice.
 * Chaque accesseur retombe sur la clé brute quand aucun libellé n'existe.
 */
class ExportLabels(
    private val exercises: Map<String, String> = emptyMap(),
    private val fields: Map<Pair<String, String>, String> = emptyMap(),
    private val questions: Map<Pair<String, String>, String> = emptyMap(),
    private val slots: Map<Pair<String, String>, String> = emptyMap(),
    val playedFirst: String = "Jouée en premier",
    val rubricTitle: String = "Grille",
    val yes: String = "oui",
    val no: String = "non",
) {
    fun exercise(id: String): String = exercises[id] ?: id

    fun field(
        exerciseId: String,
        key: String,
    ): String = fields[exerciseId to key] ?: key

    fun question(
        exerciseId: String,
        key: String,
    ): String = questions[exerciseId to key] ?: key

    fun slot(
        exerciseId: String,
        key: String,
    ): String = slots[exerciseId to key] ?: key

    companion object {
        /** Table clé vers libellé construite depuis les définitions ; [resolve] lit `strings.xml`. */
        fun from(
            registry: ExerciseRegistry,
            resolve: (StringResId) -> String,
        ): ExportLabels {
            val definitions = registry.all
            return ExportLabels(
                playedFirst = resolve(R.string.export_played_first),
                rubricTitle = resolve(R.string.export_rubric),
                yes = resolve(R.string.export_yes),
                no = resolve(R.string.export_no),
                exercises = definitions.associate { it.id.value to resolve(it.name) },
                fields =
                    definitions.keyed(resolve) {
                        it.allFields().mapNotNull { f ->
                            f.label?.let { l -> f.key to l }
                        }
                    },
                questions = definitions.keyed(resolve) { d -> d.rubric.map { it.key to it.text } },
                slots =
                    definitions.keyed(resolve) { d ->
                        d.slots.filterIsInstance<TableSlot>().mapNotNull { s -> s.label?.let { s.key to it } }
                    },
            )
        }

        private fun List<ExerciseDefinition>.keyed(
            resolve: (StringResId) -> String,
            entries: (ExerciseDefinition) -> List<Pair<String, StringResId>>,
        ): Map<Pair<String, String>, String> =
            flatMap { d -> entries(d).map { (key, res) -> (d.id.value to key) to resolve(res) } }.toMap()

        private fun ExerciseDefinition.allFields(): List<FieldSpec> =
            when (val l = layout) {
                is FieldLayout.Fixed -> l.fields
                is FieldLayout.Single -> listOf(l.field)
                is FieldLayout.Sequential -> listOf(l.field)
                is FieldLayout.RepeatableGroup -> l.before + l.fields + l.after
                is FieldLayout.None -> emptyList()
            } + listOfNotNull(endNote)
    }
}
