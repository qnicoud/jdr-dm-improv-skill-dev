package io.github.qnicoud.dmgym.platform

import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import io.github.qnicoud.dmgym.core.i18n.LanguageProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Langue de contenu déduite de la configuration de l'application (ADR 0016 §1) : la première langue de la liste
 * qui est livrée, sinon le français. Aucune préférence n'est stockée ; le choix par app (D55) passera par
 * `setApplicationLocales`, qui modifie cette même configuration.
 */
class ConfigurationLanguageProvider(
    application: Application,
) : LanguageProvider {
    private val state = MutableStateFlow(application.resources.configuration.contentLanguage())

    override val current: StateFlow<ContentLanguage> = state

    init {
        application.registerComponentCallbacks(
            object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: Configuration) {
                    state.value = newConfig.contentLanguage()
                }

                @Deprecated("Deprecated in Java")
                override fun onLowMemory() = Unit
            },
        )
    }
}
