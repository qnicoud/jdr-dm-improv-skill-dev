package io.github.qnicoud.dmgym.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Ouvre la feuille de partage d'Android avec [text] (ACTION_SEND, texte brut). Aucune permission : c'est l'appli
 * choisie par l'utilisateur qui reçoit le texte (D42). Sans application pour le recevoir, rien ne se passe.
 */
fun Context.shareText(
    text: String,
    chooserTitle: String,
) {
    val send =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
    try {
        startActivity(Intent.createChooser(send, chooserTitle))
    } catch (_: ActivityNotFoundException) {
        // Aucune application de partage installée : rien à faire.
    }
}
