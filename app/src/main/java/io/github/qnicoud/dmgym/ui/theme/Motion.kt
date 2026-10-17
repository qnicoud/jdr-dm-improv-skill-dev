package io.github.qnicoud.dmgym.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Vrai si l'utilisateur veut moins d'animations (réglage « Réduire les animations », ou le système quand le réglage
 * suit le téléphone). Fourni par [DmGymTheme] ; les transitions de navigation le respectent, et toute animation
 * ajoutée plus tard doit faire de même.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }
