package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionStatus
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Conversions entre la session du moteur et les enregistrements de l'historique (ADR 0012, architecture §4). */
private val optionsSerializer = MapSerializer(String.serializer(), Boolean.serializer())
private const val EVENT_PREFIX = "event:"

/** Options et événements déjà déclenchés, en un seul JSON (`event:<id>` pour ces derniers). */
fun ExerciseSession.optionsJson(): String =
    Json.encodeToString(optionsSerializer, options + firedEvents.associate { "$EVENT_PREFIX$it" to true })

fun ExerciseSession.drawRecords(): List<DrawRecord> =
    draws.map {
        DrawRecord(
            slotKey = it.slotKey,
            position = it.position,
            tableId = it.tableId,
            entryId = it.entryId,
            textSnapshot = it.text,
            autoDetected = it.check?.auto,
            manualChecked = it.check?.manual,
        )
    }

fun ExerciseSession.answerRecords(): List<AnswerRecord> =
    answers.map { (key, answer) ->
        AnswerRecord(key.fieldKey, key.groupIndex, answer.text, answer.flagged, answer.isGem)
    }

fun ExerciseSession.rubricRecords(): List<RubricRecord> = rubric.map { (key, value) -> RubricRecord(key, value) }

/** Reconstruit la session à la reprise : une session terminée revient en [Phase.Ended], sinon en [Phase.Active]. */
fun SessionDetail.toExerciseSession(): ExerciseSession {
    val stored = decodeOptions(session.optionsJson)
    return ExerciseSession(
        definitionId = ExerciseId(session.exerciseId),
        sessionId = session.id,
        phase = if (session.status == SessionStatus.COMPLETED) Phase.Ended else Phase.Active,
        draws = draws.map { it.toDraw() },
        answers = answers.associate { AnswerKey(it.fieldKey, it.groupIndex) to Answer(it.text, it.flagged, it.isGem) },
        options = stored.filterKeys { !it.startsWith(EVENT_PREFIX) },
        firedEvents =
            stored.keys
                .filter { it.startsWith(EVENT_PREFIX) }
                .map { it.removePrefix(EVENT_PREFIX) }
                .toSet(),
        selfRating = session.selfRating,
        isGem = session.isGem,
        rubric = rubric.associate { it.questionKey to it.value },
    )
}

private fun DrawRecord.toDraw(): Draw {
    val check =
        when {
            autoDetected != null -> ElementCheck(autoDetected, manualChecked)
            manualChecked != null -> ElementCheck(auto = false, manual = manualChecked)
            else -> null
        }
    return Draw(slotKey, position, tableId, entryId, textSnapshot, check)
}

private fun decodeOptions(json: String): Map<String, Boolean> =
    runCatching { Json.decodeFromString(optionsSerializer, json) }.getOrDefault(emptyMap())
