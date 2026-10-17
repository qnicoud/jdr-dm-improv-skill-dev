package io.github.qnicoud.dmgym

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.qnicoud.dmgym.app.DmGymApp
import io.github.qnicoud.dmgym.app.ThemeBootstrap
import io.github.qnicoud.dmgym.core.settings.ThemePalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as DmGymApplication).container
        val bootstrap = ThemeBootstrap(container.settingsRepository, lifecycleScope)
        splash.setKeepOnScreenCondition { !bootstrap.isReady }
        setContent {
            val palette = bootstrap.palette.collectAsStateWithLifecycle().value ?: ThemePalette.PARCHMENT
            DmGymApp(container, palette = palette)
        }
    }
}
