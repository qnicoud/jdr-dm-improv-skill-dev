package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Sous cette hauteur de corps d'écran (paysage, clavier ouvert), la zone épinglée passe en version compacte. */
internal val COMPACT_HEIGHT = 360.dp

/** Vrai quand la hauteur disponible sous la barre du haut est trop faible pour la zone épinglée complète. */
internal fun isCompactHeight(bodyHeight: Dp): Boolean = bodyHeight < COMPACT_HEIGHT
