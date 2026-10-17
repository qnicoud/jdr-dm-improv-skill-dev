package io.github.qnicoud.dmgym.platform

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.StyleRes
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.app.SplashThemeSync
import io.github.qnicoud.dmgym.app.SplashVariant

@StyleRes
internal fun SplashVariant.styleRes(): Int =
    when (this) {
        SplashVariant.PARCHMENT_LIGHT -> R.style.Theme_DmGym_Splash_ParchmentLight
        SplashVariant.PARCHMENT_DARK -> R.style.Theme_DmGym_Splash_ParchmentDark
        SplashVariant.FOREST_LIGHT -> R.style.Theme_DmGym_Splash_ForestLight
        SplashVariant.FOREST_DARK -> R.style.Theme_DmGym_Splash_ForestDark
        SplashVariant.NIGHT_LIGHT -> R.style.Theme_DmGym_Splash_NightLight
        SplashVariant.NIGHT_DARK -> R.style.Theme_DmGym_Splash_NightDark
        SplashVariant.MIST_LIGHT -> R.style.Theme_DmGym_Splash_MistLight
        SplashVariant.MIST_DARK -> R.style.Theme_DmGym_Splash_MistDark
    }

/**
 * Mémorise côté système le thème de l'écran de démarrage du prochain lancement (Android 13+). En dessous,
 * l'écran de démarrage garde le thème du manifeste (fond brique).
 */
fun Activity.rememberSplashTheme(variant: SplashVariant) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val prefs = getSharedPreferences(SPLASH_PREFS, Context.MODE_PRIVATE)
    SplashThemeSync(
        load = { prefs.getString(KEY_VARIANT, null) },
        save = { prefs.edit().putString(KEY_VARIANT, it).apply() },
        apply = { splashScreen.setSplashScreenTheme(it.styleRes()) },
    ).onVariant(variant)
}

private const val SPLASH_PREFS = "splash"
private const val KEY_VARIANT = "variant"
