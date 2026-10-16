package io.github.qnicoud.dmgym.core.settings

import kotlinx.coroutines.flow.Flow

/** Port des réglages (ADR 0013). */
interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun update(transform: (Settings) -> Settings)
}
