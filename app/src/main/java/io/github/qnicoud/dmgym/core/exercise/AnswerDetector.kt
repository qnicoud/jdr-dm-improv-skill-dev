package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.i18n.ContentLanguage

/**
 * Point d'extension de la détection pendant la frappe (ADR 0008). L'exercice qui déclare une
 * [DetectionSpec] reçoit à chaque changement du champ surveillé les [draws] concernés et le [text] courant ;
 * le résultat dit, pour chaque tirage, s'il apparaît dans le texte. Un tirage absent du résultat n'est pas
 * évalué : sa case reste telle quelle.
 */
fun interface AnswerDetector {
    /** Détecteur à utiliser pour une séance tirée en [language] (les mots-clés dépendent de la langue, ADR 0016 §4). */
    fun forLanguage(language: ContentLanguage): AnswerDetector = this

    /** Charge ce dont [detect] a besoin pour ces [draws] (mots-clés). À appeler avant la première frappe. */
    suspend fun prepare(draws: List<Draw>) = Unit

    fun detect(
        draws: List<Draw>,
        text: String,
    ): Map<DrawKey, Boolean>

    /** Nombre d'occurrences par tirage évalué ; vide si le détecteur ne les compte pas (alors 1 si détecté). */
    fun occurrences(
        draws: List<Draw>,
        text: String,
    ): Map<DrawKey, Int> = emptyMap()

    /** Plages (bornes incluses) du [text] à surligner, fusionnées et triées. Synchrone : appelé à chaque rendu. */
    fun highlights(
        draws: List<Draw>,
        text: String,
    ): List<IntRange> = emptyList()
}
