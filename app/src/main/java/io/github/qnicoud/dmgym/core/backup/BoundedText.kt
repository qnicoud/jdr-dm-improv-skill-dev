package io.github.qnicoud.dmgym.core.backup

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Tampon dont on lit les octets sans les recopier. */
private class ExposedBuffer : ByteArrayOutputStream() {
    fun view(): ByteBuffer = ByteBuffer.wrap(buf, 0, count)
}

/** Le flux dépasse la taille maximale autorisée. */
class TooLargeException(
    maxBytes: Int,
) : IOException("Input is larger than $maxBytes bytes")

/**
 * Lecture bornée d'un fichier non fiable (ADR 0015 §3). On compte les octets lus au lieu de se fier à une taille
 * annoncée par le fournisseur de fichiers, et le UTF-8 est strict : une séquence invalide est une erreur.
 */
object BoundedText {
    /** 8 Mio : environ trois sessions par jour pendant trois ans tiennent dans 10 Mo, mais le décodage coûte cher en mémoire. */
    const val MAX_BYTES = 8 * 1024 * 1024
    private const val CHUNK = 8 * 1024

    /**
     * @throws TooLargeException au-delà de [maxBytes] octets (la lecture s'arrête aussitôt)
     * @throws java.nio.charset.CharacterCodingException si ce n'est pas du UTF-8 valide
     */
    fun read(
        stream: InputStream,
        maxBytes: Int = MAX_BYTES,
        checkActive: () -> Unit = {},
    ): String {
        val out = ExposedBuffer()
        val buffer = ByteArray(CHUNK)
        var total = 0
        while (true) {
            checkActive()
            val n = stream.read(buffer)
            if (n < 0) break
            total += n
            if (total > maxBytes) throw TooLargeException(maxBytes)
            out.write(buffer, 0, n)
        }
        return Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(out.view())
            .toString()
    }
}
