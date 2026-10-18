package io.github.qnicoud.dmgym.core.reminder

/** Port du planificateur (ADR 0014). `sync` est idempotente : elle remplace toute alarme existante. */
interface ReminderScheduler {
    suspend fun sync(schedule: ReminderSchedule)
}
