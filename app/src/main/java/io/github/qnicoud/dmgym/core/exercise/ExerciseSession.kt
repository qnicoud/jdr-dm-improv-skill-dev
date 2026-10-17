package io.github.qnicoud.dmgym.core.exercise

enum class Phase { Ready, Active, Ended }

/** Clé de réponse du « sujet libre » (mode dégradé quand la table est illisible, DÉCISION 12). */
const val FREE_SUBJECT_KEY = "free_subject"

/** Une réponse est adressée par champ et, pour les groupes répétables, par numéro de groupe. */
data class AnswerKey(
    val fieldKey: String,
    val groupIndex: Int = 0,
)

data class Answer(
    val text: String = "",
    val flagged: Boolean = false,
    val isGem: Boolean = false,
)

/** Case d'un élément tiré : détection automatique et correction manuelle (ADR 0008). */
data class ElementCheck(
    val auto: Boolean,
    val manual: Boolean? = null,
) {
    val checked: Boolean get() = manual ?: auto
}

/**
 * Un élément tiré. [position] est son rang dans son [slotKey]. [category] est la catégorie de l'entrée de la table
 * (le sens pour `sensory_words`), `null` si l'entrée n'en a pas ou pour une carte.
 */
data class Draw(
    val slotKey: String,
    val position: Int,
    val tableId: String?,
    val entryId: String,
    val text: String,
    val check: ElementCheck? = null,
    val category: String? = null,
)

/** Identifie un tirage dans la session. */
data class DrawKey(
    val slotKey: String,
    val position: Int,
)

/** État immuable d'une séance d'exercice, transformé par [reduce]. */
data class ExerciseSession(
    val definitionId: ExerciseId,
    val sessionId: Long? = null,
    val phase: Phase = Phase.Ready,
    val draws: List<Draw> = emptyList(),
    val answers: Map<AnswerKey, Answer> = emptyMap(),
    val currentQuestion: Int = 0,
    val options: Map<String, Boolean> = emptyMap(),
    val firedEvents: Set<String> = emptySet(),
    /** Nombre de relances, sans plafond (DÉCISION 3). Gardé en mémoire seulement. */
    val rerollCount: Int = 0,
    val selfRating: Int? = null,
    val isGem: Boolean = false,
    val rubric: Map<String, Boolean> = emptyMap(),
) {
    /** Contenu au sens de l'ADR 0011 : au moins une réponse non vide après `trim`. */
    val hasContent: Boolean get() = answers.values.any { it.text.isNotBlank() }

    fun drawsOf(slotKey: String): List<Draw> = draws.filter { it.slotKey == slotKey }

    fun textOf(key: AnswerKey): String = answers[key]?.text.orEmpty()
}
