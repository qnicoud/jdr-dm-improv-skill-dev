package io.github.qnicoud.dmgym.core.backup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.CharacterCodingException

class BoundedTextTest {
    @Test
    fun `reads utf-8 text`() {
        assertEquals("é à 😀", BoundedText.read(ByteArrayInputStream("é à 😀".toByteArray())))
    }

    @Test
    fun `a stream of exactly the maximum is accepted`() {
        assertEquals(10, BoundedText.read(ByteArrayInputStream(ByteArray(10) { 'a'.code.toByte() }), 10).length)
    }

    @Test
    fun `one byte over the maximum is refused`() {
        assertThrows(TooLargeException::class.java) {
            BoundedText.read(ByteArrayInputStream(ByteArray(11)), 10)
        }
    }

    @Test
    fun `the 8 MiB limit is refused without reading the whole stream`() {
        var served = 0L
        val endless =
            object : InputStream() {
                override fun read(): Int = 'a'.code

                override fun read(
                    b: ByteArray,
                    off: Int,
                    len: Int,
                ): Int {
                    served += len
                    return len
                }
            }

        assertThrows(TooLargeException::class.java) { BoundedText.read(endless) }

        assertEquals(true, served < BoundedText.MAX_BYTES + 16 * 1024)
    }

    @Test
    fun `the maximum is 8 MiB`() {
        assertEquals(8 * 1024 * 1024, BoundedText.MAX_BYTES)
    }

    @Test
    fun `reading stops when the caller is cancelled`() {
        var checks = 0

        assertThrows(IllegalStateException::class.java) {
            BoundedText.read(ByteArrayInputStream(ByteArray(100_000)), checkActive = {
                if (++checks >
                    2
                ) {
                    error("cancelled")
                }
            })
        }
    }

    @Test
    fun `invalid utf-8 is an error`() {
        assertThrows(CharacterCodingException::class.java) {
            BoundedText.read(ByteArrayInputStream(byteArrayOf(0x7B, 0xC3.toByte(), 0x28, 0x7D)))
        }
    }
}
