package io.github.qnicoud.dmgym.core.backup

import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackupEncoderTest {
    private fun encode(vararg sessions: SessionDetail): JsonObject =
        Json.parseToJsonElement(BackupEncoder.encode(sessions.toList(), BACKUP_NOW, "1.1.0")).jsonObject

    @Test
    fun `header carries the format name, version, export date and app version`() {
        val root = encode(fullSession())

        assertEquals("dmgym-sessions", root.getValue("format").jsonPrimitive.content)
        assertEquals(1, root.getValue("formatVersion").jsonPrimitive.int)
        assertEquals("2026-10-18T14:03:00Z", root.getValue("exportedAt").jsonPrimitive.content)
        assertEquals("1.1.0", root.getValue("appVersion").jsonPrimitive.content)
    }

    @Test
    fun `a session keeps every field, including the locale constant`() {
        val session =
            encode(fullSession())
                .getValue("sessions")
                .jsonArray
                .single()
                .jsonObject

        assertEquals("crazy_eights", session.getValue("exerciseId").jsonPrimitive.content)
        assertEquals("fr", session.getValue("contentLocale").jsonPrimitive.content)
        assertEquals("Europe/Paris", session.getValue("zoneId").jsonPrimitive.content)
        assertEquals(
            EPOCH_DAY_2026_10_17,
            session
                .getValue("localEpochDay")
                .jsonPrimitive.content
                .toLong(),
        )
        assertTrue(
            session
                .getValue("options")
                .jsonObject
                .getValue("situation_mode")
                .jsonPrimitive.boolean,
        )
        assertEquals(2, session.getValue("draws").jsonArray.size)
        assertEquals(2, session.getValue("rubric").jsonArray.size)
    }

    @Test
    fun `empty drafts are not exported but unfinished sessions with content are`() {
        val unfinished =
            fullSession(START_2026_10_17 + 5_000).withSession { it.copy(status = SessionStatus.IN_PROGRESS) }

        val sessions = encode(fullSession(), inProgressWithoutContent(), unfinished).getValue("sessions").jsonArray

        assertEquals(2, sessions.size)
    }

    @Test
    fun `sessions are written oldest first and the output is compact`() {
        val text = BackupEncoder.encode(listOf(fullSession(3_000_000_000_000), fullSession()), BACKUP_NOW, "1.1.0")

        assertFalse(text.contains("\n  "))
        assertTrue(text.indexOf("$START_2026_10_17") < text.indexOf("3000000000000"))
    }

    @Test
    fun `an options column that is not valid json is exported as no options`() {
        val broken = fullSession().withSession { it.copy(optionsJson = "not json") }

        val session =
            encode(broken)
                .getValue("sessions")
                .jsonArray
                .single()
                .jsonObject

        assertTrue(session.getValue("options").jsonObject.isEmpty())
    }

    @Test
    fun `file name is ascii and dated`() {
        assertEquals(
            "dm-gym-sauvegarde-2026-10-18.json",
            BackupEncoder.fileName(java.time.LocalDate.of(2026, 10, 18)),
        )
    }
}
