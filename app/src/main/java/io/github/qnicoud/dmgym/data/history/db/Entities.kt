package io.github.qnicoud.dmgym.data.history.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [
        Index("localEpochDay"),
        Index("exerciseId", "startedAtEpochMillis"),
        Index("isGem"),
    ],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val optionsJson: String,
    val selfRating: Int?,
    val isGem: Boolean,
    val fromDaily: Boolean,
    val drawSeed: Long,
    /** Langue du contenu tiré (ADR 0016) ; `fr` pour toutes les sessions d'avant le schéma v3. */
    @ColumnInfo(defaultValue = "'fr'") val contentLocale: String = "fr",
)

@Entity(
    tableName = "session_draws",
    primaryKeys = ["sessionId", "slotKey", "position"],
    foreignKeys = [
        ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class DrawEntity(
    val sessionId: Long,
    val slotKey: String,
    val position: Int,
    val tableId: String?,
    val entryId: String,
    val textSnapshot: String,
    val autoDetected: Boolean?,
    val manualChecked: Boolean?,
    val category: String?,
)

@Entity(
    tableName = "session_answers",
    primaryKeys = ["sessionId", "fieldKey", "groupIndex"],
    foreignKeys = [
        ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class AnswerEntity(
    val sessionId: Long,
    val fieldKey: String,
    val groupIndex: Int,
    val text: String,
    val flagged: Boolean,
    val isGem: Boolean,
)

@Entity(
    tableName = "session_rubric",
    primaryKeys = ["sessionId", "questionKey"],
    foreignKeys = [
        ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class RubricEntity(
    val sessionId: Long,
    val questionKey: String,
    val value: Boolean,
)
