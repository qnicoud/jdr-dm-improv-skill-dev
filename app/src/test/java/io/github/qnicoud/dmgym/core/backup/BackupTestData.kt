package io.github.qnicoud.dmgym.core.backup

import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import java.time.Instant

val BACKUP_NOW: Instant = Instant.parse("2026-10-18T14:03:00Z")
val KNOWN_EXERCISES = setOf("crazy_eights", "constraint_box", "core_image")
const val START_2026_10_17 = 1_792_220_400_000L // 2026-10-17T07:00:00Z
const val EPOCH_DAY_2026_10_17 = 20_743L

/** Une session complète : tous les champs sont renseignés, pour qu'un aller-retour les éprouve tous. */
fun fullSession(
    startedAt: Long = START_2026_10_17,
    exerciseId: String = "crazy_eights",
    id: Long = 0,
) = SessionDetail(
    session =
        SessionRecord(
            id = id,
            exerciseId = exerciseId,
            status = SessionStatus.COMPLETED,
            startedAtEpochMillis = startedAt,
            lastActivityAtEpochMillis = startedAt + 480_000,
            endedAtEpochMillis = startedAt + 492_000,
            zoneId = "Europe/Paris",
            localEpochDay = EPOCH_DAY_2026_10_17,
            plannedDurationSec = 480,
            activeDurationSec = 470,
            overtimeSec = 12,
            difficulty = 2,
            optionsJson = """{"situation_mode":true,"event:minute":true}""",
            selfRating = 3,
            isGem = true,
            fromDaily = true,
            drawSeed = 123_456_789,
        ),
    draws =
        listOf(
            DrawRecord("item", 0, "sensory_words", "poussiereux", "poussiéreux", true, false, "sight"),
            DrawRecord("item", 1, null, "manual-1", "Une \"idée\"\nsur deux lignes", null, null, null),
        ),
    answers =
        listOf(
            AnswerRecord("idea", 0, "Texte avec accents é à ç et emoji 😀", flagged = true, isGem = true),
            AnswerRecord("idea", 1, "", flagged = false, isGem = false),
        ),
    rubric = listOf(RubricRecord("beyond_first_ideas", true), RubricRecord("first_pick:idea", true)),
)

fun SessionDetail.withSession(change: (SessionRecord) -> SessionRecord) = copy(session = change(session))

fun inProgressWithoutContent(startedAt: Long = START_2026_10_17 + 1_000) =
    fullSession(startedAt)
        .withSession { it.copy(status = SessionStatus.IN_PROGRESS, endedAtEpochMillis = null) }
        .copy(answers = emptyList())

/** Les sessions comparées sans leur `id` local. */
fun List<SessionDetail>.withoutIds() = map { it.withSession { s -> s.copy(id = 0) } }
