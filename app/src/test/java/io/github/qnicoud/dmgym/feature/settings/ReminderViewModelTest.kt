package io.github.qnicoud.dmgym.feature.settings

import app.cash.turbine.test
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.app.MutableClock
import io.github.qnicoud.dmgym.core.reminder.RecordingNotifier
import io.github.qnicoud.dmgym.core.reminder.RecordingScheduler
import io.github.qnicoud.dmgym.core.reminder.ReminderDelivery
import io.github.qnicoud.dmgym.core.reminder.ReminderSettings
import io.github.qnicoud.dmgym.core.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModelTest {
    private val clock = MutableClock(Instant.parse("2026-10-14T10:00:00Z"), ZoneId.of("UTC"))
    private val today = LocalDate.of(2026, 10, 14).toEpochDay()
    private val settings = FakeSettingsRepository()
    private val practiced = MutableStateFlow<List<Long>>(listOf(today - 1))
    private val scheduler = RecordingScheduler()
    private val notifier = RecordingNotifier()
    private val gate = FakeNotificationPermissionGate()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): ReminderViewModel {
        val history = FakeHistoryRepository(practicedDays = practiced, sessionCount = MutableStateFlow(3))
        val delivery = ReminderDelivery(settings, history, notifier, scheduler, clock)
        return ReminderViewModel(settings, history, delivery, gate, clock)
    }

    private fun reminder() = settings.state.value.reminder

    @Test
    fun `starts off with the documented defaults`() {
        val state = viewModel().uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.enabled)
        assertEquals(ReminderSettings.DEFAULT_DAYS, state.days)
        assertEquals(1110, state.minuteOfDay)
    }

    @Test
    fun `switch on with permission enables, stamps the start day and arms`() {
        val vm = viewModel()

        vm.onSwitch(true)

        assertTrue(reminder().enabled)
        assertEquals(today, reminder().resumedEpochDay)
        assertTrue(scheduler.synced.last().enabled)
        assertTrue(vm.uiState.value.enabled)
    }

    @Test
    fun `switch on without the runtime permission asks for it and stays off`() =
        runTest {
            gate.allowed = false
            gate.needsRuntimePermission = true
            val vm = viewModel()
            vm.effects.test {
                vm.onSwitch(true)

                assertEquals(ReminderEffect.RequestPermission, awaitItem())
            }
            assertFalse(reminder().enabled)
        }

    @Test
    fun `granted result enables the reminder`() {
        gate.allowed = false
        gate.needsRuntimePermission = true
        val vm = viewModel()
        vm.onSwitch(true)

        gate.allowed = true
        vm.onPermissionResult(granted = true)

        assertTrue(reminder().enabled)
        assertFalse(vm.uiState.value.permissionDenied)
    }

    @Test
    fun `denied result keeps the switch off and shows the message`() {
        gate.allowed = false
        gate.needsRuntimePermission = true
        val vm = viewModel()

        vm.onPermissionResult(granted = false)

        assertFalse(reminder().enabled)
        assertTrue(vm.uiState.value.permissionDenied)
        assertTrue(scheduler.synced.isEmpty())
    }

    @Test
    fun `granted but notifications blocked in Android counts as denied`() {
        gate.allowed = false
        val vm = viewModel()

        vm.onPermissionResult(granted = true)

        assertFalse(reminder().enabled)
        assertTrue(vm.uiState.value.permissionDenied)
    }

    @Test
    fun `before Android 13 blocked notifications show the message without asking`() =
        runTest {
            gate.allowed = false
            gate.needsRuntimePermission = false
            val vm = viewModel()
            vm.effects.test {
                vm.onSwitch(true)
                expectNoEvents()
            }
            assertTrue(vm.uiState.value.permissionDenied)
        }

    @Test
    fun `coming back from Android settings clears the message but leaves the switch off`() {
        gate.allowed = false
        val vm = viewModel()
        vm.onPermissionResult(granted = false)

        gate.allowed = true
        vm.refresh()

        assertFalse(vm.uiState.value.permissionDenied)
        assertFalse(vm.uiState.value.enabled)
    }

    @Test
    fun `switch off disarms`() {
        val vm = viewModel()
        vm.onSwitch(true)

        vm.onSwitch(false)

        assertFalse(reminder().enabled)
        assertFalse(scheduler.synced.last().enabled)
    }

    @Test
    fun `permission removed while enabled shows the blocked state and keeps the intent`() {
        val vm = viewModel()
        vm.onSwitch(true)

        gate.allowed = false
        vm.refresh()

        assertTrue(vm.uiState.value.enabled)
        assertTrue(vm.uiState.value.blocked)
    }

    @Test
    fun `time and days apply immediately`() {
        val vm = viewModel()
        vm.onSwitch(true)

        vm.setTime(hour = 7, minute = 5)
        vm.toggleDay(DayOfWeek.TUESDAY)

        assertEquals(425, reminder().minuteOfDay)
        assertTrue(DayOfWeek.TUESDAY in reminder().days)
        assertEquals(
            425,
            scheduler.synced
                .last()
                .time.hour * 60 +
                scheduler.synced
                    .last()
                    .time.minute,
        )
    }

    @Test
    fun `deselecting the last day is ignored and explained`() {
        val vm = viewModel()
        vm.onSwitch(true)
        ReminderSettings.DEFAULT_DAYS.drop(1).forEach(vm::toggleDay)

        vm.toggleDay(DayOfWeek.MONDAY)

        assertEquals(setOf(DayOfWeek.MONDAY), reminder().days)
        assertTrue(vm.uiState.value.keepOneDayHint)

        vm.toggleDay(DayOfWeek.FRIDAY)
        assertFalse(vm.uiState.value.keepOneDayHint)
    }

    @Test
    fun `fourteen idle days show the paused state and resume restarts the count`() {
        settings.state.value = Settings(reminder = ReminderSettings(enabled = true, resumedEpochDay = today - 30))
        practiced.value = listOf(today - 20)
        val vm = viewModel()
        assertTrue(vm.uiState.value.paused)

        vm.resume()

        assertFalse(vm.uiState.value.paused)
        assertEquals(today, reminder().resumedEpochDay)
    }

    @Test
    fun `a recent session is not paused`() {
        settings.state.value = Settings(reminder = ReminderSettings(enabled = true, resumedEpochDay = today - 30))
        assertFalse(viewModel().uiState.value.paused)
    }

    @Test
    fun `send test publishes without the policy`() {
        practiced.value = listOf(today)
        val vm = viewModel()

        vm.sendTest()

        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun `send test while notifications are blocked shows the message instead`() {
        gate.allowed = false
        val vm = viewModel()

        vm.sendTest()

        assertTrue(notifier.shown.isEmpty())
        assertTrue(vm.uiState.value.permissionDenied)
    }
}

class FakeNotificationPermissionGate : NotificationPermissionGate {
    var allowed = true
    override var needsRuntimePermission = false
    var openedAppSettings = 0
    var openedChannelSettings = 0

    override fun notificationsAllowed(): Boolean = allowed

    override fun openAppNotificationSettings() {
        openedAppSettings++
    }

    override fun openChannelSettings() {
        openedChannelSettings++
    }
}
