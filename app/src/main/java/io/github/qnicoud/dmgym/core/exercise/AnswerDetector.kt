package io.github.qnicoud.dmgym.core.exercise

/**
 * Point d'extension de la détection pendant la frappe (ADR 0008). L'exercice qui déclare une
 * [DetectionSpec] reçoit à chaque changement du champ surveillé les [draws] concernés et le [text] courant ;
 * le résultat dit, pour chaque tirage, s'il apparaît dans le texte. Un tirage absent du résultat n'est pas
 * évalué : sa case reste telle quelle.
 */
fun interface AnswerDetector {
    /** Charge ce dont [detect] a besoin pour ces [draws] (mots-clés). À appeler avant la première frappe. */
    suspend fun prepare(draws: List<Draw>) = Unit

    fun detect(
        draws: List<Draw>,
        text: String,
    ): Map<DrawKey, Boolean>

    /** Plages (bornes incluses) du [text] à surligner, fusionnées et triées. Synchrone : appelé à chaque rendu. */
    fun highlights(
        draws: List<Draw>,
        text: String,
    ): List<IntRange> = emptyList()
}
