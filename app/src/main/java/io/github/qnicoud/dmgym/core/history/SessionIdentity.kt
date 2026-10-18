package io.github.qnicoud.dmgym.core.history

/**
 * Identité d'une session d'un appareil à l'autre (ADR 0015, D65) : l'exercice et l'instant de début, à la
 * milliseconde. L'`id` local ne convient pas, puisque deux installations produisent les mêmes 1, 2, 3…
 */
data class SessionIdentity(
    val exerciseId: String,
    val startedAtEpochMillis: Long,
)

val SessionRecord.identity: SessionIdentity get() = SessionIdentity(exerciseId, startedAtEpochMillis)

/**
 * Sessions réellement ajoutées par un import. [insertedIds] sert à l'annuler ; [alreadyPresent] compte celles qui
 * existaient déjà au moment de l'écriture (la base peut avoir changé depuis l'aperçu).
 */
data class ImportResult(
    val insertedIds: List<Long>,
    val alreadyPresent: Int,
)
