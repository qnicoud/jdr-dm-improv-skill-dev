package io.github.qnicoud.dmgym.core.reminder

import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.app.MutableClock
import io.github.qnicoud.dmgym.core.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId

class ReminderDeliveryTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val clock = MutableClock(Instant.parse("2026-10-14T16:30:00Z"), zone)
    private val today = 20_740L // 2026-10-14

    private val scheduler = RecordingScheduler()
    private val notifier = RecordingNotifier()

    private fun delivery(
        reminder: ReminderSettings = ReminderSettings(enabled = true),
        practiced: List<Long> = listOf(today - 2),
        sessions: Int = 4,
    ) = ReminderDelivery(
        settings = FakeSettingsRepository(Settings(reminder = reminder)),
        history =
            FakeHistoryRepository(
                sessionCount = MutableStateFlow(sessions),
                practicedDays = MutableStateFlow(practiced),
            ),
        notifier = notifier,
        scheduler = scheduler,
        clock = clock,
    )

    @Test
    fun `fixture day matches the clock`() {
        assertEquals(
            today,
            java.time.LocalDate
                .of(2026, 10, 14)
                .toEpochDay(),
        )
    }

    @Test
    fun `alarm notifies then re-arms`() =
        runBlocking {
            delivery().onAlarm()

            assertEquals(1, notifier.shown.size)
            assertEquals(1, scheduler.synced.size)
            assertTrue(scheduler.synced.single().enabled)
        }

    @Test
    fun `a session today means no notification but the next occurrence is armed`() =
        runBlocking {
            delivery(practiced = listOf(today)).onAlarm()

            assertTrue(notifier.shown.isEmpty())
            assertEquals(1, scheduler.synced.size)
        }

    @Test
    fun `fourteen idle days pause the reminder without notification`() =
        runBlocking {
            delivery(practiced = listOf(today - 14)).onAlarm()

            assertTrue(notifier.shown.isEmpty())
            assertEquals(1, scheduler.synced.size)
        }

    @Test
    fun `no session yet picks the first exercise text`() =
        runBlocking {
            delivery(practiced = emptyList(), sessions = 0).onAlarm()

            assertTrue(notifier.shown.single().firstExercise)
        }

    @Test
    fun `a stale alarm after deactivation cancels and stays silent`() =
        runBlocking {
            delivery(reminder = ReminderSettings(enabled = false)).onAlarm()

            assertTrue(notifier.shown.isEmpty())
            assertEquals(false, scheduler.synced.single().enabled)
        }

    @Test
    fun `the notifier failing does not prevent re-arming`() =
        runBlocking {
            notifier.fail = true

            runCatching { delivery().onAlarm() }

            assertEquals(1, scheduler.synced.size)
        }

    @Test
    fun `resync pushes the stored schedule`() =
        runBlocking {
            delivery(reminder = ReminderSettings(enabled = true, minuteOfDay = 600)).resync()

            assertEquals(java.time.LocalTime.of(10, 0), scheduler.synced.single().time)
        }

    @Test
    fun `test notification skips the policy`() {
        delivery(practiced = listOf(today)).sendTest()

        assertEquals(ReminderDecision.Notify(textIndex = 0, firstExercise = false), notifier.shown.single())
    }
}

class RecordingScheduler : ReminderScheduler {
    val synced = mutableListOf<ReminderSchedule>()

    override suspend fun sync(schedule: ReminderSchedule) {
        synced += schedule
    }
}

class RecordingNotifier : ReminderNotifier {
    val shown = mutableListOf<ReminderDecision.Notify>()
    var fail = false

    override fun show(notification: ReminderDecision.Notify) {
        check(!fail) { "channel blocked" }
        shown += notification
    }
}
