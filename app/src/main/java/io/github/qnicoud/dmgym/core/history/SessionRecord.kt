package io.github.qnicoud.dmgym.core.history

import io.github.qnicoud.dmgym.core.i18n.ContentLanguage

enum class SessionStatus { IN_PROGRESS, COMPLETED }

/** Séance d'exercice. `localEpochDay` est écrit une seule fois, à la création (ADR 0011). */
data class SessionRecord(
    val id: Long = 0,
    val exerciseId: String,
    val status: SessionStatus,
    val startedAtEpochMillis: Long,
    /** Dernière écriture de contenu ; le délai de 24 h (D36) se compte depuis cet instant. */
    val lastActivityAtEpochMillis: Long = startedAtEpochMillis,
    val endedAtEpochMillis: Long? = null,
    val zoneId: String,
    val localEpochDay: Long,
    val plannedDurationSec: Int? = null,
    val activeDurationSec: Int = 0,
    val overtimeSec: Int = 0,
    val difficulty: Int = 1,
    val optionsJson: String = "{}",
    val selfRating: Int? = null,
    val isGem: Boolean = false,
    val fromDaily: Boolean = false,
    val drawSeed: Long = 0,
    /** Code de la langue du contenu tiré (`ContentLanguage.code`) ; fixée à la création de la séance (ADR 0016 §3). */
    val contentLocale: String = ContentLanguage.DEFAULT.code,
)

data class DrawRecord(
    val slotKey: String,
    val position: Int,
    val tableId: String?,
    val entryId: String,
    val textSnapshot: String,
    val autoDetected: Boolean? = null,
    val manualChecked: Boolean? = null,
    /** Catégorie de l'entrée tirée (sens d'un mot sensoriel, type d'élément d'histoire), pour l'afficher sans relire la table. */
    val category: String? = null,
)

data class AnswerRecord(
    val fieldKey: String,
    val groupIndex: Int = 0,
    val text: String,
    val flagged: Boolean = false,
    val isGem: Boolean = false,
)

data class RubricRecord(
    val questionKey: String,
    val value: Boolean,
)

data class ExerciseCountRecord(
    val exerciseId: String,
    val count: Int,
)

/** Session avec ses tirages, réponses et réponses de grille. */
data class SessionDetail(
    val session: SessionRecord,
    val draws: List<DrawRecord>,
    val answers: List<AnswerRecord>,
    val rubric: List<RubricRecord>,
)
