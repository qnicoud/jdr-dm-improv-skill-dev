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

/** Condition sur une option de l'exercice : un tirage ou un champ n'existe que si elle est remplie. */
data class OptionCondition(
    val option: String,
    val expected: Boolean = true,
) {
    /** Une option absente de [options] prend sa valeur dans [defaults] (celle de la définition), sinon `false`. */
    fun holds(
        options: Map<String, Boolean>,
        defaults: Map<String, Boolean> = emptyMap(),
    ): Boolean = (options[option] ?: defaults[option] ?: false) == expected
}

/** Un tirage de l'écran « Prêt » (ADR 0012). */
sealed interface DrawSlot {
    val key: String

    /** Le slot n'est tiré (et affiché) que si la condition est remplie ; `null` : toujours. */
    val onlyIf: OptionCondition?
}

/** Vrai si le tirage du slot change quand l'option [option] change (mode, contre-stéréotype). */
fun DrawSlot.dependsOn(option: String): Boolean =
    onlyIf?.option == option || (this is CardSlot && contrast?.option == option)

/** Les slots tirés pour les [options] données. */
fun List<DrawSlot>.activeFor(options: Map<String, Boolean>): List<DrawSlot> =
    filter { it.onlyIf?.holds(options) ?: true }

/**
 * Étiquette à préférer pour un tirage de table, selon le statut de la carte tirée dans [cardSlot] : un statut haut
 * cherche [high], un statut bas [low], un statut moyen (ou une carte absente) n'a pas de préférence. La préférence
 * cède si la table n'a pas assez d'entrées étiquetées.
 */
data class StatusTagRule(
    val cardSlot: String,
    val high: String,
    val low: String,
)

/** Tirage de [count] entrées d'une table, sans remise. */
data class TableSlot(
    override val key: String,
    val table: TableId,
    val count: Int = 1,
    val category: String? = null,
    val distinctBy: DistinctBy = DistinctBy.Id,
    val difficulty: IntRange = 1..3,
    /** Si non vide, la i-ème entrée vient de la catégorie `i % taille` (questions « dans l'ordre des catégories »). */
    val categoryCycle: List<String> = emptyList(),
    override val onlyIf: OptionCondition? = null,
    /** Libellé affiché devant chaque tirage du slot (par ex. « Lieu »), s'il y en a un. */
    val label: StringResId? = null,
    /** Étiquette préférée selon le statut de la carte déjà tirée (bascule de Status Numbers). */
    val tagByStatus: StatusTagRule? = null,
) : DrawSlot {
    init {
        require(count >= 1) { "count must be >= 1" }
    }
}

/** La carte suit le rang (`rank:*`) du rôle tiré dans [roleSlot], à l'opposé quand [option] est active (D45). */
data class RankContrast(
    val roleSlot: String,
    val option: String,
)

/** Carte numérotée (Status Numbers). */
data class CardSlot(
    override val key: String,
    val range: IntRange = 1..10,
    val contrast: RankContrast? = null,
    override val onlyIf: OptionCondition? = null,
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
    override val onlyIf: OptionCondition? = null,
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
    /** Le champ n'est affiché que si la condition est remplie. */
    val visibleWhen: OptionCondition? = null,
    /** Plafond conseillé de caractères : compteur souple, jamais bloquant. */
    val softLimit: Int? = null,
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

    /**
     * Pas de saisie pendant le jeu (Status Numbers) : un grand bouton [finishLabel] termine la scène ; les notes
     * éventuelles sont saisies dans le bilan ([ExerciseDefinition.endNote]).
     */
    data class None(
        val finishLabel: StringResId,
    ) : FieldLayout

    /** [before] et [after] encadrent les groupes (contexte, « si les PJ ne font rien ») ; ils portent le groupe 0. */
    data class RepeatableGroup(
        val fields: List<FieldSpec>,
        val max: Int,
        val before: List<FieldSpec> = emptyList(),
        val after: List<FieldSpec> = emptyList(),
        /** Titre d'un groupe (reçoit son numéro) et libellé du bouton d'ajout ; sinon les libellés génériques. */
        val title: StringResId? = null,
        val addLabel: StringResId? = null,
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
    /** Court texte d'aide affiché sous l'interrupteur. */
    val help: StringResId? = null,
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
    /** L'événement n'a lieu que si cette option est active (option « bascule »). */
    val requiresOption: String? = null,
)

/**
 * Libellé du bouton de départ quand l'option de [whenOption] est remplie. Le texte reçoit la durée du minuteur
 * (« Pause faite : lancer 30 s »).
 */
data class StartVariant(
    val label: StringResId,
    val whenOption: OptionCondition,
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
    /** Notes facultatives saisies dans le bilan (Status Numbers). */
    val endNote: FieldSpec? = null,
    /** Vrai si le panneau des éléments tirés se replie quand le clavier s'ouvre (Connect the Story, D48). */
    val collapsiblePanel: Boolean = false,
    /** Libellé particulier du bouton de départ selon une option (Decision Point en mode vidéo). */
    val startVariant: StartVariant? = null,
    /** Phrase neutre posée dans le bilan (Decision Point : « Laquelle aurais-tu jouée en premier ? »). */
    val endPrompt: StringResId? = null,
    /** Suggestion du bilan tant qu'aucune réponse n'est marquée du drapeau (Decision Point : case « jamais »). */
    val endFlagHint: StringResId? = null,
    /** Vrai si le bilan propose de choisir l'une des réponses du formulaire (Decision Point, [FirstPick]). */
    val endChoice: Boolean = false,
) {
    init {
        require(rubric.size in RUBRIC_SIZE) { "rubric must have 2 or 3 questions" }
        require(slots.map { it.key }.distinct().size == slots.size) { "slot keys must be unique" }
    }

    /** Valeur des options de la session, complétée par les valeurs par défaut de celles qui manquent. */
    fun resolveOptions(stored: Map<String, Boolean>): Map<String, Boolean> =
        options.associate { it.key to it.default } + stored

    /** La variante du bouton de départ valable pour les options de la session, `null` : libellé habituel. */
    fun startVariantFor(stored: Map<String, Boolean>): StartVariant? =
        startVariant?.takeIf { it.whenOption.holds(resolveOptions(stored)) }

    /** Clés des champs de saisie, dans l'ordre. */
    val fieldKeys: List<String>
        get() =
            when (layout) {
                is FieldLayout.Fixed -> layout.fields.map { it.key }
                is FieldLayout.Single -> listOf(layout.field.key)
                is FieldLayout.Sequential -> listOf(layout.field.key)
                is FieldLayout.RepeatableGroup -> (layout.before + layout.fields + layout.after).map { it.key }
                is FieldLayout.None -> emptyList()
            }

    private companion object {
        val RUBRIC_SIZE = 2..3
    }
}
