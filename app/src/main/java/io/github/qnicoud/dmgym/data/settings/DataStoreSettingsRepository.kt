package io.github.qnicoud.dmgym.data.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.qnicoud.dmgym.core.daily.decodeReplacements
import io.github.qnicoud.dmgym.core.daily.encodeReplacements
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.timer.DurationBounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Réglages dans DataStore Preferences (ADR 0013).
 *
 * @param durationDefaults durée par défaut de chaque exercice : sert aux bornes (écriture et lecture).
 * @param untimedByDefault exercices « sans minuteur » tant qu'aucune valeur n'est enregistrée (One-Page).
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val durationDefaults: Map<ExerciseId, Duration> = emptyMap(),
    private val untimedByDefault: Set<ExerciseId> = emptySet(),
) : SettingsRepository {
    override val settings: Flow<Settings> =
        dataStore.data
            .catch { error ->
                if (error !is IOException) throw error
                Log.e(TAG, "Settings unreadable, using defaults", error)
                emit(emptyPreferences())
            }.map { read(it) }

    override suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { prefs -> write(prefs, transform(read(prefs))) }
    }

    private fun read(prefs: Preferences): Settings {
        val values = prefs.asMap().mapKeys { it.key.name }
        return Settings(
            soundEnabled = values.bool(SOUND, default = true),
            vibrationEnabled = values.bool(VIBRATION, default = true),
            durations = readDurations(values),
            untimed = readUntimed(values),
            dailyIncludeOnePage = values.bool(DAILY_ONE_PAGE, default = false),
            dailyEpochDay = values[DAILY_EPOCH_DAY] as? Long,
            dailyReplacements = decodeReplacements(values[DAILY_REPLACEMENTS] as? String),
            crazyEightsMinuteHaptic = values.bool(CRAZY_MINUTE_HAPTIC, default = false),
        )
    }

    private fun readDurations(values: Map<String, Any>): Map<ExerciseId, Duration> =
        values
            .filterKeys { it.startsWith(DURATION_PREFIX) }
            .mapNotNull { (key, value) ->
                val id = key.removePrefix(DURATION_PREFIX).takeIf { it.isNotBlank() }?.let(::ExerciseId)
                val duration = (value as? Int)?.takeIf { it > 0 }?.seconds
                if (id != null && duration != null && isValid(id, duration)) id to duration else null
            }.toMap()

    private fun readUntimed(values: Map<String, Any>): Set<ExerciseId> {
        val explicit =
            values
                .filterKeys { it.startsWith(UNTIMED_PREFIX) }
                .mapNotNull { (key, value) ->
                    val id = key.removePrefix(UNTIMED_PREFIX).takeIf { it.isNotBlank() }
                    if (id != null && value is Boolean) ExerciseId(id) to value else null
                }.toMap()
        return (untimedByDefault.filter { it !in explicit } + explicit.filterValues { it }.keys).toSet()
    }

    private fun isValid(
        id: ExerciseId,
        duration: Duration,
    ): Boolean = durationDefaults[id]?.let { duration in DurationBounds.of(it) } ?: true

    private fun write(
        prefs: MutablePreferences,
        settings: Settings,
    ) {
        prefs[booleanPreferencesKey(SOUND)] = settings.soundEnabled
        prefs[booleanPreferencesKey(VIBRATION)] = settings.vibrationEnabled
        prefs[booleanPreferencesKey(DAILY_ONE_PAGE)] = settings.dailyIncludeOnePage
        prefs[booleanPreferencesKey(CRAZY_MINUTE_HAPTIC)] = settings.crazyEightsMinuteHaptic
        writeDaily(prefs, settings)
        writeDurations(prefs, settings.durations)
        writeUntimed(prefs, settings.untimed)
    }

    private fun writeDaily(
        prefs: MutablePreferences,
        settings: Settings,
    ) {
        val epochDayKey = longPreferencesKey(DAILY_EPOCH_DAY)
        val replacementsKey = stringPreferencesKey(DAILY_REPLACEMENTS)
        val day = settings.dailyEpochDay
        if (day != null && settings.dailyReplacements.isNotEmpty()) {
            prefs[epochDayKey] = day
            prefs[replacementsKey] = encodeReplacements(settings.dailyReplacements)
        } else {
            prefs.remove(epochDayKey)
            prefs.remove(replacementsKey)
        }
    }

    private fun writeDurations(
        prefs: MutablePreferences,
        durations: Map<ExerciseId, Duration>,
    ) {
        prefs
            .asMap()
            .keys
            .filter { it.name.startsWith(DURATION_PREFIX) }
            .forEach { prefs.remove(it) }
        durations.forEach { (id, duration) ->
            val clamped = durationDefaults[id]?.let { DurationBounds.of(it).clamp(duration) } ?: duration
            if (clamped.isPositive()) {
                prefs[intPreferencesKey(DURATION_PREFIX + id.value)] =
                    clamped.inWholeSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }
        }
    }

    private fun writeUntimed(
        prefs: MutablePreferences,
        untimed: Set<ExerciseId>,
    ) {
        prefs
            .asMap()
            .keys
            .filter { it.name.startsWith(UNTIMED_PREFIX) }
            .forEach { prefs.remove(it) }
        (untimed + untimedByDefault).forEach { id ->
            prefs[booleanPreferencesKey(UNTIMED_PREFIX + id.value)] = id in untimed
        }
    }

    private fun Map<String, Any>.bool(
        key: String,
        default: Boolean,
    ): Boolean = this[key] as? Boolean ?: default

    companion object {
        private const val TAG = "SettingsRepository"
        private const val SOUND = "sound_enabled"
        private const val VIBRATION = "vibration_enabled"
        private const val DAILY_ONE_PAGE = "daily_include_one_page"
        private const val DAILY_EPOCH_DAY = "daily_epoch_day"
        private const val DAILY_REPLACEMENTS = "daily_replacements"
        private const val CRAZY_MINUTE_HAPTIC = "crazy_eights_minute_haptic"
        private const val DURATION_PREFIX = "duration_s."
        private const val UNTIMED_PREFIX = "untimed."
        private const val FILE_NAME = "settings"

        /** Crée le DataStore unique `settings.preferences_pb` (à ne créer qu'une fois par processus). */
        fun createDataStore(
            context: Context,
            scope: CoroutineScope,
        ): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
            ) { context.preferencesDataStoreFile(FILE_NAME) }
    }
}
