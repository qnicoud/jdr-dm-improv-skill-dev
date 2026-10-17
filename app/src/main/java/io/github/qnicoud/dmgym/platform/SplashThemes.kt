package io.github.qnicoud.dmgym.platform

import android.app.Activity
import android.os.Build
import androidx.annotation.StyleRes
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.app.SplashVariant

@StyleRes
internal fun SplashVariant.styleRes(): Int =
    when (this) {
        SplashVariant.PARCHMENT -> R.style.Theme_DmGym_Splash_Parchment
        SplashVariant.FOREST -> R.style.Theme_DmGym_Splash_Forest
        SplashVariant.NIGHT -> R.style.Theme_DmGym_Splash_Night
        SplashVariant.MIST -> R.style.Theme_DmGym_Splash_Mist
    }

/**
 * Mémorise côté système le thème de l'écran de démarrage du prochain lancement (Android 13+). En dessous,
 * l'écran de démarrage garde le thème du manifeste (fond brique).
 */
fun Activity.rememberSplashTheme(variant: SplashVariant) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        splashScreen.setSplashScreenTheme(variant.styleRes())
    }
}
