package io.github.qnicoud.dmgym.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.qnicoud.dmgym.core.reminder.ReminderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files
import java.time.DayOfWeek

/** Clés du rappel (ADR 0014 §4). */
@RunWith(RobolectricTestRunner::class)
class DataStoreReminderSettingsTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var file: File
    private lateinit var store: DataStore<Preferences>

    @Before
    fun setUp() {
        file = File(Files.createTempDirectory("reminder").toFile(), "settings.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    @After
    fun tearDown() {
        scope.cancel()
        file.parentFile?.deleteRecursively()
    }

    private fun repository() = DataStoreSettingsRepository(store)

    @Test
    fun `an empty store gives the reminder defaults`() =
        runBlocking {
            assertEquals(ReminderSettings(), repository().settings.first().reminder)
        }

    @Test
    fun `reminder settings are persisted and read back`() =
        runBlocking {
            val reminder =
                ReminderSettings(
                    enabled = true,
                    minuteOfDay = 7 * 60 + 15,
                    days = setOf(DayOfWeek.TUESDAY, DayOfWeek.SUNDAY),
                    resumedEpochDay = 20_000L,
                )
            repository().update { it.copy(reminder = reminder) }

            assertEquals(reminder, repository().settings.first().reminder)
            val prefs = store.data.first()
            assertEquals(true, prefs[booleanPreferencesKey("reminder_enabled")])
            assertEquals(435, prefs[intPreferencesKey("reminder_minute_of_day")])
            assertEquals("2,7", prefs[stringPreferencesKey("reminder_days")])
            assertEquals(20_000L, prefs[longPreferencesKey("reminder_resumed_epoch_day")])
        }

    @Test
    fun `an absent resume day removes the key`() =
        runBlocking {
            val repo = repository()
            repo.update { it.copy(reminder = ReminderSettings(resumedEpochDay = 5)) }
            repo.update { it.copy(reminder = ReminderSettings(resumedEpochDay = null)) }

            assertNull(store.data.first()[longPreferencesKey("reminder_resumed_epoch_day")])
        }

    @Test
    fun `corrupt reminder values fall back to the defaults`() =
        runBlocking {
            store.edit {
                it[intPreferencesKey("reminder_minute_of_day")] = 5000
                it[stringPreferencesKey("reminder_days")] = "x,0,9"
                it[stringPreferencesKey("reminder_enabled")] = "oui"
                it[stringPreferencesKey("reminder_resumed_epoch_day")] = "hier"
            }

            assertEquals(ReminderSettings(), repository().settings.first().reminder)
        }
}
