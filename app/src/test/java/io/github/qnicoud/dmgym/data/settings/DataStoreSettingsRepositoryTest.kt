package io.github.qnicoud.dmgym.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.qnicoud.dmgym.core.daily.replacementsFor
import io.github.qnicoud.dmgym.core.daily.withReplacement
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class DataStoreSettingsRepositoryTest {
    private val crazy = ExerciseId("crazy_eights")
    private val onePage = ExerciseId("one_page")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var file: File
    private lateinit var store: DataStore<Preferences>

    @Before
    fun setUp() {
        file = File(Files.createTempDirectory("settings").toFile(), "settings.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    @After
    fun tearDown() {
        scope.cancel()
        file.parentFile?.deleteRecursively()
    }

    private fun repository(store: DataStore<Preferences> = this.store) =
        DataStoreSettingsRepository(
            dataStore = store,
            durationDefaults = mapOf(crazy to 8.minutes, onePage to 15.minutes),
            untimedByDefault = setOf(onePage),
        )

    @Test
    fun `an empty store gives the documented defaults`() =
        runBlocking {
            val settings = repository().settings.first()

            assertEquals(
                Settings(
                    soundEnabled = true,
                    vibrationEnabled = true,
                    durations = emptyMap(),
                    untimed = setOf(onePage),
                    dailyIncludeOnePage = false,
                    crazyEightsMinuteHaptic = false,
                ),
                settings,
            )
        }

    @Test
    fun `updates are persisted and read back`() =
        runBlocking {
            val repo = repository()

            repo.update {
                it.copy(
                    soundEnabled = false,
                    vibrationEnabled = false,
                    dailyIncludeOnePage = true,
                    crazyEightsMinuteHaptic = true,
                    durations = mapOf(crazy to 10.minutes),
                )
            }

            val read = repository().settings.first()
            assertFalse(read.soundEnabled)
            assertFalse(read.vibrationEnabled)
            assertTrue(read.dailyIncludeOnePage)
            assertTrue(read.crazyEightsMinuteHaptic)
            assertEquals(mapOf(crazy to 10.minutes), read.durations)
        }

    @Test
    fun `durations are clamped to the bounds on write`() =
        runBlocking {
            val repo = repository()

            repo.update { it.copy(durations = mapOf(crazy to 1.seconds, onePage to 1000.minutes)) }

            val read = repo.settings.first()
            assertEquals(4.minutes, read.durations[crazy])
            assertEquals(150.minutes, read.durations[onePage])
        }

    @Test
    fun `removing a duration restores the default`() =
        runBlocking {
            val repo = repository()
            repo.update { it.copy(durations = mapOf(crazy to 10.minutes)) }

            repo.update { it.copy(durations = emptyMap()) }

            assertEquals(emptyMap<ExerciseId, Duration>(), repo.settings.first().durations)
        }

    @Test
    fun `an out of bounds stored duration is ignored on read`() =
        runBlocking {
            store.edit {
                it[intPreferencesKey("duration_s.crazy_eights")] = 5
                it[intPreferencesKey("duration_s.one_page")] = 1200
                it[intPreferencesKey("duration_s.unknown")] = -3
            }

            val read = repository().settings.first()

            assertEquals(mapOf(onePage to 20.minutes), read.durations)
        }

    @Test
    fun `untimed defaults on for one page and can be switched off explicitly`() =
        runBlocking {
            val repo = repository()
            assertEquals(setOf(onePage), repo.settings.first().untimed)

            repo.update { it.copy(untimed = emptySet()) }
            assertEquals(emptySet<ExerciseId>(), repo.settings.first().untimed)

            repo.update { it.copy(untimed = setOf(crazy, onePage)) }
            assertEquals(setOf(crazy, onePage), repo.settings.first().untimed)
        }

    @Test
    fun `a huge duration is capped instead of overflowing`() =
        runBlocking {
            val repo = DataStoreSettingsRepository(store)

            repo.update { it.copy(durations = mapOf(crazy to Duration.INFINITE)) }

            assertTrue(
                repo.settings
                    .first()
                    .durations
                    .getValue(crazy) > 1000.minutes,
            )
        }

    @Test
    fun `a value of the wrong type falls back to the default`() =
        runBlocking {
            store.edit {
                it[intPreferencesKey("sound_enabled")] = 3
                it[stringPreferencesKey("untimed.crazy_eights")] = "oui"
            }

            val read = repository().settings.first()

            assertTrue(read.soundEnabled)
            assertFalse(read.untimed.contains(crazy))
        }

    @Test
    fun `a corrupted file yields the defaults instead of crashing`() =
        runBlocking {
            file.writeBytes(byteArrayOf(0x7f, 0x01, 0x42, 0x13, 0x55))
            val corrupted =
                PreferenceDataStoreFactory.create(
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                    scope = scope,
                    produceFile = { file },
                )
            val repo = repository(corrupted)

            assertEquals(Settings(untimed = setOf(onePage)), repo.settings.first())
            repo.update { it.copy(soundEnabled = false) }
            assertFalse(repo.settings.first().soundEnabled)
        }

    @Test
    fun `daily replacements and their day are persisted`() =
        runBlocking {
            val repo = repository()

            repo.update {
                it
                    .withReplacement(
                        20_000,
                        1,
                        ExerciseId.STATUS_NUMBERS,
                    ).withReplacement(20_000, 2, ExerciseId.CORE_IMAGE)
            }

            val read = repository().settings.first()
            assertEquals(20_000L, read.dailyEpochDay)
            assertEquals(
                mapOf(1 to ExerciseId.STATUS_NUMBERS, 2 to ExerciseId.CORE_IMAGE),
                read.replacementsFor(20_000),
            )
            assertEquals(emptyMap<Int, ExerciseId>(), read.replacementsFor(20_001))
        }

    @Test
    fun `daily replacements are stored in the documented format`() =
        runBlocking {
            repository().update { it.withReplacement(7, 0, ExerciseId.STATUS_NUMBERS) }

            val prefs = store.data.first()
            assertEquals(7L, prefs[longPreferencesKey("daily_epoch_day")])
            assertEquals("0:status_numbers", prefs[stringPreferencesKey("daily_replacements")])
        }

    @Test
    fun `clearing the daily replacements removes the keys`() =
        runBlocking {
            val repo = repository()
            repo.update { it.withReplacement(7, 0, ExerciseId.STATUS_NUMBERS) }

            repo.update { it.copy(dailyEpochDay = null, dailyReplacements = emptyMap()) }

            val prefs = store.data.first()
            assertEquals(null, prefs[longPreferencesKey("daily_epoch_day")])
            assertEquals(null, prefs[stringPreferencesKey("daily_replacements")])
            assertEquals(Settings(untimed = setOf(onePage)), repo.settings.first())
        }

    @Test
    fun `unreadable daily values fall back to no replacement`() =
        runBlocking {
            store.edit {
                it[stringPreferencesKey("daily_epoch_day")] = "hier"
                it[intPreferencesKey("daily_replacements")] = 4
            }

            val read = repository().settings.first()

            assertEquals(null, read.dailyEpochDay)
            assertEquals(emptyMap<Int, ExerciseId>(), read.dailyReplacements)
        }
}
