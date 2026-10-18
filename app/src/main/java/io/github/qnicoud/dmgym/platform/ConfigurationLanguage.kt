package io.github.qnicoud.dmgym.platform

import android.content.res.Configuration
import androidx.core.os.ConfigurationCompat
import io.github.qnicoud.dmgym.core.i18n.ContentLanguage

/** Langue de contenu d'une configuration : première langue de la liste qui est livrée, sinon le français. */
fun Configuration.contentLanguage(): ContentLanguage {
    val locales = ConfigurationCompat.getLocales(this)
    return ContentLanguage.resolve((0 until locales.size()).mapNotNull { locales[it]?.toLanguageTag() })
}
