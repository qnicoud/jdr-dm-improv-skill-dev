package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import io.github.qnicoud.dmgym.core.exercise.Draw

/**
 * Surlignage des mots imposés dans le champ (ADR 0008). L'information ne passe jamais par la couleur seule :
 * gras et soulignement accompagnent le fond. Le texte n'est pas modifié, donc aucun décalage de curseur.
 * Le dernier résultat est mémorisé : un rendu sans nouvelle frappe ne relance pas la détection.
 */
internal class HighlightTransformation(
    private val ranges: (String) -> List<IntRange>,
    private val style: SpanStyle,
) : OutputTransformation {
    private var lastText: String? = null
    private var lastRanges: List<IntRange> = emptyList()

    override fun TextFieldBuffer.transformOutput() {
        val text = asCharSequence().toString()
        spanBounds(forText(text), length).forEach { (start, end) -> addStyle(style, start, end) }
    }

    private fun forText(text: String): List<IntRange> {
        if (text != lastText) {
            lastRanges = ranges(text)
            lastText = text
        }
        return lastRanges
    }
}

/** Bornes (fin exclue) des plages, ramenées dans le texte ; une plage vide après ajustement est écartée. */
internal fun spanBounds(
    ranges: List<IntRange>,
    length: Int,
): List<Pair<Int, Int>> =
    ranges
        .map { it.first.coerceIn(0, length) to (it.last + 1).coerceIn(0, length) }
        .filter { (start, end) -> start < end }

/** Fournit à chaque champ sa transformation de surlignage (ou `null`), sans alourdir les signatures. */
internal val LocalFieldHighlight = compositionLocalOf<(String) -> OutputTransformation?> { { null } }

/**
 * Transformation de surlignage du champ [fieldKey], recréée quand les tirages changent (relance) pour que le champ
 * se redessine avec les nouveaux mots.
 */
@Composable
internal fun rememberHighlight(
    detectionField: String?,
    draws: List<Draw>,
    ranges: ((String) -> List<IntRange>)?,
): (String) -> OutputTransformation? {
    val style =
        SpanStyle(
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline,
            background = MaterialTheme.colorScheme.tertiaryContainer,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    val drawsKey = draws.map { it.entryId }
    val transformation =
        remember(ranges, drawsKey, style, detectionField) {
            if (ranges == null || detectionField == null) null else HighlightTransformation(ranges, style)
        }
    return remember(transformation, detectionField) {
        { fieldKey -> transformation?.takeIf { fieldKey == detectionField } }
    }
}
