package io.github.qnicoud.dmgym.platform

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * Écrit [text] (UTF-8) dans le document choisi par l'utilisateur via le sélecteur système (`ACTION_CREATE_DOCUMENT`).
 * Aucune permission : l'accès vient du `Uri` renvoyé par le sélecteur. Lève une exception si l'écriture échoue.
 */
suspend fun ContentResolver.writeText(
    uri: Uri,
    text: String,
) = withContext(Dispatchers.IO) {
    val stream = openOutputStream(uri, "wt") ?: throw FileNotFoundException("No output stream for $uri")
    stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
}
