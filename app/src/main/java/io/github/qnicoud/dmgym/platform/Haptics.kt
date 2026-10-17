package io.github.qnicoud.dmgym.platform

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** Court retour haptique du rappel périodique. Passe par la vue : aucune permission de vibration. */
fun View.performTick() {
    val constant =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
    performHapticFeedback(constant)
}
