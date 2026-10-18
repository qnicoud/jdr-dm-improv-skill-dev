package io.github.qnicoud.dmgym.core.backup

import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Classes de transfert de la version 1 du format (ADR 0015 §2). **Figées** : ajouter, retirer ou renommer un champ
 * demande une nouvelle version (`BackupV2`) et de nouveaux fichiers témoins. Elles ne dépendent ni de Room ni des
 * classes du domaine, pour que l'évolution du schéma de la base ne change jamais ce qu'un fichier v1 veut dire.
 * L'ordre des champs est celui du fichier écrit.
 */
@Serializable
internal data class BackupV1(
    val format: String,
    val formatVersion: Int,
    val exportedAt: String,
    val appVersion: String,
    val sessions: List<SessionV1>,
)

@Serializable
internal data class SessionV1(
    val exerciseId: String,
    val status: String,
    val startedAtEpochMillis: Long,
    val lastActivityAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val zoneId: String,
    val localEpochDay: Long,
    val plannedDurationSec: Int?,
    val activeDurationSec: Int,
    val overtimeSec: Int,
    val difficulty: Int,
    val options: Map<String, Boolean>,
    val selfRating: Int?,
    val isGem: Boolean,
    val fromDaily: Boolean,
    val drawSeed: Long,
    /** Facultatif à la lecture : une sauvegarde faite avant le lot de langue reste valide (ADR 0016). */
    val contentLocale: String? = null,
    val draws: List<DrawV1>,
    val answers: List<AnswerV1>,
    val rubric: List<RubricV1>,
)

@Serializable
internal data class DrawV1(
    val slotKey: String,
    val position: Int,
    val tableId: String?,
    val entryId: String,
    val textSnapshot: String,
    val autoDetected: Boolean?,
    val manualChecked: Boolean?,
    val category: String?,
)

@Serializable
internal data class AnswerV1(
    val fieldKey: String,
    val groupIndex: Int,
    val text: String,
    val flagged: Boolean,
    val isGem: Boolean,
)

@Serializable
internal data class RubricV1(
    val questionKey: String,
    val value: Boolean,
)

private val optionsSerializer = MapSerializer(String.serializer(), Boolean.serializer())

internal fun SessionDetail.toV1(): SessionV1 =
    SessionV1(
        exerciseId = session.exerciseId,
        status = session.status.name,
        startedAtEpochMillis = session.startedAtEpochMillis,
        lastActivityAtEpochMillis = session.lastActivityAtEpochMillis,
        endedAtEpochMillis = session.endedAtEpochMillis,
        zoneId = session.zoneId,
        localEpochDay = session.localEpochDay,
        plannedDurationSec = session.plannedDurationSec,
        activeDurationSec = session.activeDurationSec,
        overtimeSec = session.overtimeSec,
        difficulty = session.difficulty,
        options = decodeOptions(session.optionsJson),
        selfRating = session.selfRating,
        isGem = session.isGem,
        fromDaily = session.fromDaily,
        drawSeed = session.drawSeed,
        contentLocale = DEFAULT_CONTENT_LOCALE,
        draws =
            draws.map {
                DrawV1(
                    it.slotKey,
                    it.position,
                    it.tableId,
                    it.entryId,
                    it.textSnapshot,
                    it.autoDetected,
                    it.manualChecked,
                    it.category,
                )
            },
        answers = answers.map { AnswerV1(it.fieldKey, it.groupIndex, it.text, it.flagged, it.isGem) },
        rubric = rubric.map { RubricV1(it.questionKey, it.value) },
    )

/** Suppose la session déjà validée ([BackupValidator]). */
internal fun SessionV1.toDomain(): SessionDetail =
    SessionDetail(
        session =
            SessionRecord(
                exerciseId = exerciseId,
                status = SessionStatus.valueOf(status),
                startedAtEpochMillis = startedAtEpochMillis,
                lastActivityAtEpochMillis = lastActivityAtEpochMillis,
                endedAtEpochMillis = endedAtEpochMillis,
                zoneId = zoneId,
                localEpochDay = localEpochDay,
                plannedDurationSec = plannedDurationSec,
                activeDurationSec = activeDurationSec,
                overtimeSec = overtimeSec,
                difficulty = difficulty,
                optionsJson = Json.encodeToString(optionsSerializer, options),
                selfRating = selfRating,
                isGem = isGem,
                fromDaily = fromDaily,
                drawSeed = drawSeed,
            ),
        draws =
            draws.map {
                DrawRecord(
                    it.slotKey,
                    it.position,
                    it.tableId,
                    it.entryId,
                    it.textSnapshot,
                    it.autoDetected,
                    it.manualChecked,
                    it.category,
                )
            },
        answers = answers.map { AnswerRecord(it.fieldKey, it.groupIndex, it.text, it.flagged, it.isGem) },
        rubric = rubric.map { RubricRecord(it.questionKey, it.value) },
    )

private fun decodeOptions(json: String): Map<String, Boolean> =
    runCatching { Json.decodeFromString(optionsSerializer, json) }.getOrDefault(emptyMap())
