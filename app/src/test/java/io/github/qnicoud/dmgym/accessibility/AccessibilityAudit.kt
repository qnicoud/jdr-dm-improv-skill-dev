package io.github.qnicoud.dmgym.accessibility

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult

/** Seuils de l'audit (design system §7). */
object AuditLimits {
    const val MIN_TARGET_DP = 48f

    /** Tolérance d'arrondi en pixels sur les tailles mesurées. */
    const val PIXEL_SLACK = 1f
}

/**
 * Audit d'accessibilité statique de l'arbre sémantique affiché : cibles tactiles, libellés, titres et troncatures.
 * Chaque méthode renvoie des violations lisibles ; l'appelant les assemble avec le nom de l'écran.
 */
class AccessibilityAudit(
    private val compose: ComposeContentTestRule,
) {
    private val clickable = SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)
    private val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    private fun density(): Float =
        compose
            .onRoot()
            .fetchSemanticsNode()
            .layoutInfo.density.density

    private fun clickableNodes(): List<SemanticsNode> =
        compose.onAllNodes(clickable).fetchSemanticsNodes().filter { it.size.width > 0 && it.size.height > 0 }

    /** Cibles tactiles (zone étendue comprise) plus petites que 48 dp. */
    fun smallTargets(): List<String> {
        val density = density()
        val min = AuditLimits.MIN_TARGET_DP * density - AuditLimits.PIXEL_SLACK
        return clickableNodes()
            .filter { it.touchBoundsInRoot.width < min || it.touchBoundsInRoot.height < min }
            .map {
                val w = it.touchBoundsInRoot.width / density
                val h = it.touchBoundsInRoot.height / density
                "${describe(it)} cible $w x $h dp"
            }
    }

    /**
     * Mesure stricte, informative : taille visuelle du nœud (sans l'extension automatique de la zone tactile).
     * TalkBack et le scanner d'accessibilité lisent ces bornes ; le rapport RC1 les liste sans faire échouer l'audit.
     */
    fun smallVisualTargets(): List<String> {
        val density = density()
        val min = AuditLimits.MIN_TARGET_DP * density - AuditLimits.PIXEL_SLACK
        return clickableNodes()
            .filter { it.size.width < min || it.size.height < min }
            .map { "${describe(it)} taille visuelle ${it.size.width / density} x ${it.size.height / density} dp" }
    }

    /** Nœuds cliquables sans texte, ni description, ni texte éditable (l'annonce serait vide). */
    fun unlabelledClickables(): List<String> =
        clickableNodes()
            .filter { node ->
                val config = node.config
                config.getOrNull(SemanticsProperties.Text).isNullOrEmpty() &&
                    config.getOrNull(SemanticsProperties.ContentDescription).isNullOrEmpty() &&
                    config.getOrNull(SemanticsProperties.EditableText) == null
            }.map { "${describe(it)} sans libellé" }

    /**
     * Textes coupés en hauteur ou tronqués par une ellipse. Le dépassement en largeur (`didOverflowWidth`) n'est pas
     * utilisé : sous Robolectric les mesures de police sont approximatives et il se déclenche à tort.
     */
    fun clippedTexts(): List<String> =
        compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult))
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config
                    .getOrNull(SemanticsActions.GetTextLayoutResult)
                    ?.action
                    ?.invoke(results)
                results.firstOrNull()?.takeIf { it.didOverflowHeight || it.isEllipsized() }?.let {
                    "${describe(node)} texte coupé (hauteur=${it.didOverflowHeight}, ellipse=${it.isEllipsized()})"
                }
            }

    private fun TextLayoutResult.isEllipsized(): Boolean = (0 until lineCount).any { isLineEllipsized(it) }

    /**
     * Champs de saisie dont TalkBack ne lirait que la valeur : ni description, ni libellé fusionné à côté du texte
     * saisi (le texte fusionné d'un champ M3 contient le libellé puis la valeur).
     */
    fun unlabelledTextFields(): List<String> =
        compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
            .fetchSemanticsNodes()
            .filter { node ->
                val config = node.config
                val value = config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()
                val texts = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
                config.getOrNull(SemanticsProperties.ContentDescription).isNullOrEmpty() &&
                    texts.none { it.isNotEmpty() && it != value }
            }.map { "${describe(it)} champ sans libellé" }

    /** Éléments à état (case, bouton radio, interrupteur, onglet) sans rôle : TalkBack ne dirait pas leur type. */
    fun statefulWithoutRole(): List<String> =
        compose
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState) or
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected),
            ).fetchSemanticsNodes()
            .filter { it.config.getOrNull(SemanticsProperties.Role) == null }
            .map { "${describe(it)} état sans rôle" }

    fun headingCount(): Int = compose.onAllNodes(heading).fetchSemanticsNodes().size

    private fun describe(node: SemanticsNode): String {
        val config = node.config
        val text =
            config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
                ?: config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
        val tag = config.getOrNull(SemanticsProperties.TestTag)
        return "[id=${node.id} tag=$tag texte=${text?.take(40)}]"
    }
}
