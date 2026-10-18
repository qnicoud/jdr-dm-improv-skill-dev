package io.github.qnicoud.dmgym.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import java.util.Locale

/**
 * Locale des formats de date et d'heure (ADR 0016 §5) : celle de la langue de l'app, qui est la première langue du
 * téléphone dont le contenu est livré, sinon le français. Elle suit donc les textes de l'interface, qui n'existent
 * que dans cette langue.
 */
@Composable
fun currentLocale(): Locale {
    val locales = LocalConfiguration.current.locales
    return remember(locales) {
        ContentLanguage.resolve((0 until locales.size()).map { locales[it].toLanguageTag() }).locale
    }
}
