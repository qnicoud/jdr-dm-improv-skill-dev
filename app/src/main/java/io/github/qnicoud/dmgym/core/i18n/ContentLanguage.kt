package io.github.qnicoud.dmgym.core.i18n

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Langues de contenu livrées (ADR 0016 §1) : une langue n'y figure que si toutes ses tables, son corpus de
 * détection et ses traductions sont présents. Français seul pour l'instant (D54).
 */
enum class ContentLanguage(
    val code: String,
) {
    FR("fr"),
    ;

    /** Locale de formatage : celle de la langue de contenu, jamais la langue brute du téléphone. */
    val locale: Locale get() = Locale.forLanguageTag(code)

    companion object {
        val DEFAULT = FR

        /** Langue d'un code stocké (`"fr"`), ou `null` s'il est inconnu. */
        fun fromCode(code: String?): ContentLanguage? = entries.firstOrNull { it.code == code }

        /**
         * Première langue prise en charge d'une liste d'étiquettes (`"fr-CA"`, `"en_GB"`), sinon [DEFAULT] : c'est le
         * repli silencieux de l'UX §4.2.
         */
        fun resolve(tags: List<String>): ContentLanguage =
            tags.firstNotNullOfOrNull { fromCode(it.substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)) }
                ?: DEFAULT
    }
}

/** Port : langue courante de l'app (celle de l'interface, si son contenu est livré). */
interface LanguageProvider {
    val current: StateFlow<ContentLanguage>

    class Fixed(
        language: ContentLanguage = ContentLanguage.DEFAULT,
    ) : LanguageProvider {
        override val current: StateFlow<ContentLanguage> = MutableStateFlow(language)
    }
}
