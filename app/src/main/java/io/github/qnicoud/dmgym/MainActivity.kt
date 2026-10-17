package io.github.qnicoud.dmgym

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.qnicoud.dmgym.app.Appearance
import io.github.qnicoud.dmgym.app.DmGymApp
import io.github.qnicoud.dmgym.app.SplashPolicy
import io.github.qnicoud.dmgym.app.SplashVariant
import io.github.qnicoud.dmgym.app.ThemeBootstrap
import io.github.qnicoud.dmgym.core.settings.resolve
import io.github.qnicoud.dmgym.platform.rememberSplashTheme
import io.github.qnicoud.dmgym.platform.systemAnimationsDisabled
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val startedAt = SystemClock.elapsedRealtime()
        val policy = SplashPolicy(isColdStart = consumeColdStart())
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as DmGymApplication).container
        val bootstrap = ThemeBootstrap(container.settingsRepository, lifecycleScope)
        splash.setKeepOnScreenCondition {
            policy.keepOnScreen(bootstrap.isReady, SystemClock.elapsedRealtime() - startedAt)
        }
        rememberSplashThemeOnChange(bootstrap)
        setContent {
            val appearance = bootstrap.appearance.collectAsStateWithLifecycle().value ?: Appearance()
            val dark = appearance.mode.isDark(isSystemInDarkTheme())
            val reduceMotion = appearance.reduceAnimations.resolve(rememberSystemReducedMotion())
            SystemBarsFollowTheme(dark)
            DmGymApp(container, palette = appearance.palette, darkTheme = dark, reduceMotion = reduceMotion)
        }
    }

    /** Le démarrage suivant prend la couleur de la palette choisie, y compris après un changement de palette. */
    private fun rememberSplashThemeOnChange(bootstrap: ThemeBootstrap) {
        lifecycleScope.launch {
            bootstrap.appearance
                .filterNotNull()
                .map { SplashVariant.of(it.palette) }
                .distinctUntilChanged()
                .collect { rememberSplashTheme(it) }
        }
    }

    /** Réglage d'animations du système, relu quand l'app revient au premier plan. */
    @Composable
    private fun rememberSystemReducedMotion(): Boolean {
        val context = LocalContext.current
        var reduced by remember { mutableStateOf(context.systemAnimationsDisabled()) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { reduced = context.systemAnimationsDisabled() }
        return reduced
    }

    /** Les icônes des barres système suivent le thème choisi, pas seulement celui du téléphone. */
    @Composable
    private fun SystemBarsFollowTheme(dark: Boolean) {
        DisposableEffect(dark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
            )
            onDispose {}
        }
    }

    private companion object {
        /** Vrai une seule fois par processus : un retour à chaud (processus vivant) n'attend pas la seconde. */
        var coldStart = true

        fun consumeColdStart(): Boolean = coldStart.also { coldStart = false }

        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
