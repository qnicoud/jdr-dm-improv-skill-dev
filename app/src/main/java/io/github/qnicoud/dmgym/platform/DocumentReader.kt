package io.github.qnicoud.dmgym.platform

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.qnicoud.dmgym.core.backup.BoundedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * Lit le document choisi par l'utilisateur (`ACTION_OPEN_DOCUMENT`), borné à [BoundedText.MAX_BYTES] octets et en
 * UTF-8 strict. Aucune permission : l'accès vient du `Uri` du sélecteur, et on ne le garde pas.
 * Lève `TooLargeException`, `CharacterCodingException`, `IOException` ou `SecurityException`.
 */
suspend fun ContentResolver.readBoundedText(uri: Uri): String =
    withContext(Dispatchers.IO) {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) throw SecurityException("Only content:// documents are read")
        val stream = openInputStream(uri) ?: throw FileNotFoundException("No input stream for $uri")
        stream.use { BoundedText.read(it, checkActive = { ensureActive() }) }
    }

/** Écrit la sauvegarde et renvoie le nom du fichier créé (celui que l'utilisateur a choisi), `uri` en repli. */
suspend fun ContentResolver.writeBackup(
    uri: Uri,
    text: String,
): String {
    writeText(uri, text)
    return withContext(Dispatchers.IO) { displayName(uri) } ?: uri.lastPathSegment.orEmpty()
}

@Suppress("TooGenericExceptionCaught")
private fun ContentResolver.displayName(uri: Uri): String? =
    try {
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }

/** Version affichée de l'app, écrite dans l'en-tête de la sauvegarde. */
fun Context.appVersionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "unknown"
