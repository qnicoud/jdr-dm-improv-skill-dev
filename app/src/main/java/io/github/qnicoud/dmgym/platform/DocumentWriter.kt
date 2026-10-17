package io.github.qnicoud.dmgym.platform

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * Écrit [text] (UTF-8) dans le document choisi par l'utilisateur via le sélecteur système (`ACTION_CREATE_DOCUMENT`).
 * Aucune permission : l'accès vient du `Uri` renvoyé par le sélecteur.
 * Si l'écriture échoue, le document partiel est supprimé (échec de suppression ignoré) et l'exception est relancée.
 */
suspend fun ContentResolver.writeText(
    uri: Uri,
    text: String,
) = withContext(Dispatchers.IO) {
    try {
        val stream = openOutputStream(uri, "wt") ?: throw FileNotFoundException("No output stream for $uri")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    } catch (e: Exception) {
        deleteQuietly(uri)
        throw e
    }
}

@Suppress("TooGenericExceptionCaught")
private fun ContentResolver.deleteQuietly(uri: Uri) {
    try {
        DocumentsContract.deleteDocument(this, uri)
    } catch (_: Exception) {
        // Au mieux : l'erreur d'origine reste celle remontée à l'utilisateur.
    }
}
