package io.github.qnicoud.dmgym.core.tables

import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TableFileTest {
    @Test
    fun `decodes the fixture with defaults`() {
        val table = TestTables.fixtureSmall()

        assertEquals("places", table.table)
        assertEquals(6, table.entries.size)
        val puits = table.entries.last()
        assertEquals(null, puits.category)
        assertEquals(emptyList<String>(), puits.keywords)
        assertFalse(puits.fuzzy)
        assertEquals(emptyList<String>(), puits.tags)
    }

    @Test
    fun `unknown key is rejected`() {
        val json = """{"schemaVersion":1,"table":"t","locale":"fr","entries":[],"extra":1}"""

        assertThrows(SerializationException::class.java) { TableFile.decode(json) }
    }

    @Test
    fun `unsupported schema version is rejected with a clear message`() {
        val json = """{"schemaVersion":2,"table":"t","locale":"fr","entries":[]}"""

        val error = assertThrows(IllegalArgumentException::class.java) { TableFile.decode(json) }

        assertTrue(error.message.orEmpty().contains("schemaVersion"))
    }

    @Test
    fun `missing difficulty is rejected`() {
        val json = """{"schemaVersion":1,"table":"t","locale":"fr","entries":[{"id":"a","text":"b"}]}"""

        assertThrows(SerializationException::class.java) { TableFile.decode(json) }
    }

    @Test
    fun `table ids map to unique file names`() {
        assertEquals(
            14,
            TableId.entries
                .map { it.fileName }
                .toSet()
                .size,
        )
    }
}
