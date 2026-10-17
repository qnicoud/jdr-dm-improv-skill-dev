package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.tables.DistinctBy
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.TimedEvent
import kotlin.time.Duration

/**
 * Identifiant d'une ressource `strings.xml`. `core` n'a pas le droit de référencer `androidx` (garde-fou
 * d'architecture) : l'annotation `@StringRes` n'est donc pas utilisée ici.
 */
typealias StringResId = Int

/** Politique de minuteur d'un exercice (ADR 0006, 0012). */
sealed interface TimerPolicy {
    data class Required(
        val default: Duration,
    ) : TimerPolicy

    data class Optional(
        val suggested: Duration,
    ) : TimerPolicy

    data object None : TimerPolicy
}

/** Durée par défaut portée par la politique, `null` si elle n'en a pas. */
val TimerPolicy.defaultDuration: Duration?
    get() =
        when (this) {
            is TimerPolicy.Required -> default
            is TimerPolicy.Optional -> suggested
            TimerPolicy.None -> null
        }

/** Un tirage de l'écran « Prêt » (ADR 0012). */
sealed interface DrawSlot {
    val key: String
}

/** Tirage de [count] entrées d'une table, sans remise. */
data class TableSlot(
    override val key: String,
    val table: TableId,
    val count: Int = 1,
    val category: String? = null,
    val distinctBy: DistinctBy = DistinctBy.Id,
    val difficulty: IntRange = 1..3,
) : DrawSlot {
    init {
        require(count >= 1) { "count must be >= 1" }
    }
}

/** Carte numérotée (Status Numbers). */
data class CardSlot(
    override val key: String,
    val range: IntRange = 1..10,
) : DrawSlot {
    init {
        require(!range.isEmpty()) { "range must not be empty" }
    }
}

/** Choix préalable ; en v1 un seul domaine, donc résolu sur [default] sans interface. */
data class ChoiceSlot(
    override val key: String,
    val choices: List<String>,
    val default: String = choices.first(),
) : DrawSlot {
    init {
        require(default in choices) { "default must be one of the choices" }
    }
}

/** Un champ de saisie. Sans [label], l'écran affiche le numéro d'ordre. */
data class FieldSpec(
    val key: String,
    val label: StringResId? = null,
    val hint: StringResId? = null,
    val minLines: Int = 1,
    /** Libellé de la case à cocher de la réponse (« Je ne ferais jamais ça »), si l'exercice en a une. */
    val flagLabel: StringResId? = null,
)

/** Disposition de la saisie (ADR 0012). */
sealed interface FieldLayout {
    data class Fixed(
        val fields: List<FieldSpec>,
    ) : FieldLayout

    data class Single(
        val field: FieldSpec,
    ) : FieldLayout

    /** Une question à la fois ; les questions sont les tirages de [questionsSlot]. */
    data class Sequential(
        val questionsSlot: String,
        val field: FieldSpec,
    ) : FieldLayout

    data class RepeatableGroup(
        val fields: List<FieldSpec>,
        val max: Int,
    ) : FieldLayout {
        init {
            require(max >= 1) { "max must be >= 1" }
        }
    }
}

/** Détection automatique des mots dans le champ [field] pour les tirages [slotKeys] (ADR 0008). */
data class DetectionSpec(
    val slotKeys: List<String>,
    val field: String,
)

/** Règle d'affichage de la progression. */
sealed interface ProgressRule {
    data object None : ProgressRule

    /** « x/n » champs non vides parmi ceux de la disposition fixe. */
    data object FilledFields : ProgressRule
}

/** Bascule propre à l'exercice, stockée avec la session. */
data class ExerciseOption(
    val key: String,
    val label: StringResId,
    val default: Boolean = false,
)

/** Question Oui/Non de l'auto-évaluation. */
data class RubricQuestion(
    val key: String,
    val text: StringResId,
)

/**
 * Événement programmé : [timing] fixe l'instant ; si [draw] est présent, l'occurrence tire dans cette table
 * (bascule de Status Numbers).
 */
data class ScheduledEvent(
    val id: String,
    val timing: TimedEvent,
    val draw: TableSlot? = null,
)

/** Description déclarative d'un exercice (ADR 0012). */
data class ExerciseDefinition(
    val id: ExerciseId,
    val name: StringResId,
    val discipline: StringResId,
    val instructions: StringResId,
    val reminder: StringResId?,
    val timer: TimerPolicy,
    val slots: List<DrawSlot>,
    val layout: FieldLayout,
    val rubric: List<RubricQuestion>,
    val detection: DetectionSpec? = null,
    val progress: ProgressRule = ProgressRule.None,
    val events: List<ScheduledEvent> = emptyList(),
    val options: List<ExerciseOption> = emptyList(),
    val warmUp: Boolean = true,
    /** Vrai si l'exercice accepte « Écrire mon propre sujet » quand la table est illisible (DÉCISION 12). */
    val acceptsFreeSubject: Boolean = false,
) {
    init {
        require(rubric.size in RUBRIC_SIZE) { "rubric must have 2 or 3 questions" }
        require(slots.map { it.key }.distinct().size == slots.size) { "slot keys must be unique" }
    }

    /** Clés des champs de saisie, dans l'ordre. */
    val fieldKeys: List<String>
        get() =
            when (layout) {
                is FieldLayout.Fixed -> layout.fields.map { it.key }
                is FieldLayout.Single -> listOf(layout.field.key)
                is FieldLayout.Sequential -> listOf(layout.field.key)
                is FieldLayout.RepeatableGroup -> layout.fields.map { it.key }
            }

    private companion object {
        val RUBRIC_SIZE = 2..3
    }
}
