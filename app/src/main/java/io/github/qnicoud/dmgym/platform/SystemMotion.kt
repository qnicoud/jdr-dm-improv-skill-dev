package io.github.qnicoud.dmgym.platform

import android.content.Context
import android.provider.Settings

/** Vrai si le téléphone a coupé les animations (« Supprimer les animations » / échelle de durée à 0). */
fun Context.systemAnimationsDisabled(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
