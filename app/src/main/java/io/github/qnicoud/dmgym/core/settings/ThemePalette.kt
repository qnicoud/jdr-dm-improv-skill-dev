package io.github.qnicoud.dmgym.core.settings

/** Palette de l'app (ADR 0013, complément D53). Stockée par son nom ; une valeur inconnue retombe sur [PARCHMENT]. */
enum class ThemePalette {
    /** Thème d'origine « Parchemin » / « Cuir à la torche » (design-system §2). */
    PARCHMENT,
    FOREST,
    NIGHT,
    MIST,

    /** Couleurs du téléphone (Material You), à partir de l'API 31. */
    DYNAMIC,
    ;

    /** La palette réellement appliquée : [DYNAMIC] n'existe pas avant l'API 31. */
    fun effective(sdkInt: Int): ThemePalette = if (this == DYNAMIC && sdkInt < DYNAMIC_MIN_SDK) PARCHMENT else this

    companion object {
        const val DYNAMIC_MIN_SDK = 31

        /** Les palettes proposées à l'utilisateur, dans l'ordre d'affichage. */
        fun available(sdkInt: Int): List<ThemePalette> = entries.filter { it.effective(sdkInt) == it }

        fun parse(name: String?): ThemePalette = entries.firstOrNull { it.name == name } ?: PARCHMENT
    }
}
