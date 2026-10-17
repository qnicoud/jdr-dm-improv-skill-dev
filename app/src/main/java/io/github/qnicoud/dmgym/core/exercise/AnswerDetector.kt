package io.github.qnicoud.dmgym.core.exercise

/**
 * Point d'extension de la détection pendant la frappe (ADR 0008). L'exercice qui déclare une
 * [DetectionSpec] reçoit à chaque changement du champ surveillé les [draws] concernés et le [text] courant ;
 * le résultat dit, pour chaque tirage, s'il apparaît dans le texte. Implémentation réelle au lot L8.
 */
fun interface AnswerDetector {
    fun detect(
        draws: List<Draw>,
        text: String,
    ): Map<DrawKey, Boolean>
}
