package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.runtime.saveable.listSaver

/**
 * Repli du panneau des éléments (D48) : il se replie à la première ouverture du clavier seulement. Un panneau
 * déplié ensuite à la main le reste, même si le clavier se rouvre, jusqu'à ce que l'utilisateur le replie.
 */
data class PanelFold(
    val expanded: Boolean = true,
    val autoFolded: Boolean = false,
) {
    fun onKeyboardShown(): PanelFold = if (autoFolded) this else PanelFold(expanded = false, autoFolded = true)

    fun toggled(): PanelFold = copy(expanded = !expanded)

    fun toList(): List<Boolean> = listOf(expanded, autoFolded)

    companion object {
        fun fromList(values: List<Boolean>) = PanelFold(values[0], values[1])

        val Saver = listSaver(save = { it.toList() }, restore = { fromList(it) })
    }
}
