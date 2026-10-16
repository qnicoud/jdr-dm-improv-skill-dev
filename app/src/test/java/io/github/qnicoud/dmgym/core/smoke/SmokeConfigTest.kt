package io.github.qnicoud.dmgym.core.smoke

import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SmokeConfigTest {
    @Test
    fun `strict decoding succeeds on a valid document`() {
        val config = SmokeConfig.decode("""{"id":"smoke","count":3,"tags":["a","b"]}""")

        assertEquals(SmokeConfig(id = "smoke", count = 3, tags = listOf("a", "b")), config)
    }

    @Test
    fun `tags are optional`() {
        assertEquals(emptyList<String>(), SmokeConfig.decode("""{"id":"smoke","count":1}""").tags)
    }

    @Test
    fun `strict decoding fails on an unknown key`() {
        assertThrows(SerializationException::class.java) {
            SmokeConfig.decode("""{"id":"smoke","count":3,"unexpected":true}""")
        }
    }

    @Test
    fun `strict decoding fails on a missing required key`() {
        assertThrows(SerializationException::class.java) {
            SmokeConfig.decode("""{"id":"smoke"}""")
        }
    }
}
