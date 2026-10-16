package io.github.qnicoud.dmgym.data.history

import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.ExerciseCountRecord
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.data.history.db.AnswerEntity
import io.github.qnicoud.dmgym.data.history.db.DrawEntity
import io.github.qnicoud.dmgym.data.history.db.ExerciseCount
import io.github.qnicoud.dmgym.data.history.db.RubricEntity
import io.github.qnicoud.dmgym.data.history.db.SessionEntity
import io.github.qnicoud.dmgym.data.history.db.SessionWithChildren

internal fun SessionRecord.toEntity() =
    SessionEntity(
        id = id,
        exerciseId = exerciseId,
        status = status.name,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        zoneId = zoneId,
        localEpochDay = localEpochDay,
        plannedDurationSec = plannedDurationSec,
        activeDurationSec = activeDurationSec,
        overtimeSec = overtimeSec,
        difficulty = difficulty,
        optionsJson = optionsJson,
        selfRating = selfRating,
        isGem = isGem,
        fromDaily = fromDaily,
        drawSeed = drawSeed,
    )

internal fun SessionEntity.toRecord() =
    SessionRecord(
        id = id,
        exerciseId = exerciseId,
        status = SessionStatus.valueOf(status),
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        zoneId = zoneId,
        localEpochDay = localEpochDay,
        plannedDurationSec = plannedDurationSec,
        activeDurationSec = activeDurationSec,
        overtimeSec = overtimeSec,
        difficulty = difficulty,
        optionsJson = optionsJson,
        selfRating = selfRating,
        isGem = isGem,
        fromDaily = fromDaily,
        drawSeed = drawSeed,
    )

internal fun DrawRecord.toEntity(sessionId: Long) =
    DrawEntity(sessionId, slotKey, position, tableId, entryId, textSnapshot, autoDetected, manualChecked)

internal fun DrawEntity.toRecord() =
    DrawRecord(slotKey, position, tableId, entryId, textSnapshot, autoDetected, manualChecked)

internal fun AnswerRecord.toEntity(sessionId: Long) =
    AnswerEntity(sessionId, fieldKey, groupIndex, text, flagged, isGem)

internal fun AnswerEntity.toRecord() = AnswerRecord(fieldKey, groupIndex, text, flagged, isGem)

internal fun RubricRecord.toEntity(sessionId: Long) = RubricEntity(sessionId, questionKey, value)

internal fun RubricEntity.toRecord() = RubricRecord(questionKey, value)

internal fun ExerciseCount.toRecord() = ExerciseCountRecord(exerciseId, n)

internal fun SessionWithChildren.toDetail() =
    SessionDetail(
        session = session.toRecord(),
        draws = draws.map { it.toRecord() }.sortedWith(compareBy({ it.slotKey }, { it.position })),
        answers = answers.map { it.toRecord() }.sortedWith(compareBy({ it.fieldKey }, { it.groupIndex })),
        rubric = rubric.map { it.toRecord() }.sortedBy { it.questionKey },
    )
