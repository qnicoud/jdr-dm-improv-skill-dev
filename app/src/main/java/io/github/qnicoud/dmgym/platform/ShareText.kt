package io.github.qnicoud.dmgym.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Ouvre la feuille de partage d'Android avec [text] (ACTION_SEND, texte brut). Aucune permission : c'est l'appli
 * choisie par l'utilisateur qui reçoit le texte (D42). Renvoie `false` si aucune application ne peut le recevoir.
 */
fun Context.shareText(
    text: String,
    chooserTitle: String,
): Boolean {
    val send =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
    return try {
        val chooser = Intent.createChooser(send, chooserTitle)
        // Hors d'une Activity (contexte d'application), Android exige une nouvelle tâche.
        if (this !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(chooser)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
