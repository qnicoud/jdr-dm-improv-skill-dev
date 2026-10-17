package io.github.qnicoud.dmgym.core.settings

/** Mode clair ou sombre (S11, Apparence). Stocké par son nom ; une valeur inconnue retombe sur [SYSTEM]. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    fun isDark(systemDark: Boolean): Boolean =
        when (this) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }

    companion object {
        fun parse(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/** « Réduire les animations » : `null` (défaut) suit le réglage d'animations du système. */
fun Boolean?.resolve(systemReduced: Boolean): Boolean = this ?: systemReduced
