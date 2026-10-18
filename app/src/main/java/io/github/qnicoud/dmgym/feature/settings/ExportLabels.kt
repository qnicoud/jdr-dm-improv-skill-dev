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
    /** Libellé du choix du bilan par exercice (`endChoiceLabel`) ; sinon [playedFirst]. */
    private val endChoices: Map<String, String> = emptyMap(),
    /** « Offre 2 sur 5 » par exercice dont les questions sont révélées une à une ; absent : numérotation « (n) ». */
    private val itemLabels: Map<String, (Int, Int) -> String> = emptyMap(),
    /** Slot des questions révélées une à une, par exercice : ses tirages s'arrêtent à la dernière question atteinte. */
    private val questionSlots: Map<String, String> = emptyMap(),
    /** Clés des champs dans l'ordre de la définition, par exercice. */
    private val fieldOrders: Map<String, List<String>> = emptyMap(),
    val playedFirst: String = "Jouée en premier",
    val rubricTitle: String = "Grille",
    val yes: String = "oui",
    val no: String = "non",
) {
    fun exercise(id: String): String = exercises[id] ?: id

    fun itemLabel(
        exerciseId: String,
        item: Int,
        total: Int,
    ): String? = itemLabels[exerciseId]?.invoke(item, total)

    fun questionSlot(exerciseId: String): String? = questionSlots[exerciseId]

    fun fieldRank(
        exerciseId: String,
        key: String,
    ): Int = fieldOrders[exerciseId]?.indexOf(key)?.takeIf { it >= 0 } ?: Int.MAX_VALUE

    fun endChoice(exerciseId: String): String = endChoices[exerciseId] ?: playedFirst

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
            /** Résout un libellé à deux nombres (« Offre %1$d sur %2$d ») ; `null` : le libellé brut. */
            resolveItem: ((StringResId, Int, Int) -> String)? = null,
            resolve: (StringResId) -> String,
        ): ExportLabels {
            val definitions = registry.all
            val sequential = definitions.mapNotNull { d -> (d.layout as? FieldLayout.Sequential)?.let { d to it } }
            val itemized = sequential.filter { (_, l) -> l.itemLabel != null }
            return ExportLabels(
                itemLabels =
                    itemized.associate { (d, l) ->
                        val res = requireNotNull(l.itemLabel)
                        d.id.value to { item: Int, total: Int -> resolveItem?.invoke(res, item, total) ?: resolve(res) }
                    },
                questionSlots =
                    sequential.filter { (_, l) -> l.revealProgressively }.associate { (d, l) ->
                        d.id.value to
                            l.questionsSlot
                    },
                fieldOrders = sequential.associate { (d, _) -> d.id.value to d.fieldKeys },
                playedFirst = resolve(R.string.export_played_first),
                rubricTitle = resolve(R.string.export_rubric),
                yes = resolve(R.string.export_yes),
                no = resolve(R.string.export_no),
                exercises = definitions.associate { it.id.value to resolve(it.name) },
                endChoices =
                    definitions
                        .mapNotNull { d -> d.endChoiceLabel?.let { d.id.value to resolve(it) } }
                        .toMap(),
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
                is FieldLayout.Sequential -> l.fields
                is FieldLayout.RepeatableGroup -> l.before + l.fields + l.after
                is FieldLayout.None -> emptyList()
            } + listOfNotNull(endNote)
    }
}
