package io.github.qnicoud.dmgym.feature.settings

import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.ThemeMode
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import io.github.qnicoud.dmgym.core.settings.VibrationStrength
import io.github.qnicoud.dmgym.feature.exercise.FakeEndSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val repo = FakeSettingsRepository()
    private val endSignal = FakeEndSignal()
    private val archive = FakeSessionArchive()
    private var clockMillis = Instant.parse("2026-10-17T08:00:00Z").toEpochMilli()
    private val clock =
        object : Clock() {
            override fun getZone() = ZoneOffset.UTC

            override fun withZone(zone: java.time.ZoneId?) = this

            override fun instant(): Instant = Instant.ofEpochMilli(clockMillis)
        }

    private fun viewModel(sdkInt: Int = 34) =
        SettingsViewModel(repo, sdkInt, endSignal, archive, clock, UnconfinedTestDispatcher())

    private fun SettingsViewModel.row(id: ExerciseId) = uiState.value.durations.first { it.id == id }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts loading then lists the seven exercises with their defaults`() =
        runTest {
            val vm = viewModel()
            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(ExerciseId.ALL, state.durations.map { it.id })
            assertEquals(480, vm.row(ExerciseId.CRAZY_EIGHTS).seconds)
            assertTrue(vm.row(ExerciseId.ONE_PAGE).untimed)
            assertTrue(state.vibration && state.sound)
            assertFalse(state.crazyMinuteHaptic || state.dailyIncludeOnePage)
            assertEquals(ThemePalette.PARCHMENT, state.palette)
        }

    @Test
    fun `rows expose the bounds of the duration`() =
        runTest {
            val row = viewModel().row(ExerciseId.DECISION_POINT)
            assertEquals(15, row.minSeconds)
            assertEquals(300, row.maxSeconds)
        }

    @Test
    fun `increase and decrease follow the bound steps`() =
        runTest {
            val vm = viewModel()
            vm.increase(ExerciseId.DECISION_POINT)
            assertEquals(45, vm.row(ExerciseId.DECISION_POINT).seconds)
            vm.decrease(ExerciseId.DECISION_POINT)
            vm.decrease(ExerciseId.DECISION_POINT)
            assertEquals(15, vm.row(ExerciseId.DECISION_POINT).seconds)
            assertFalse(vm.row(ExerciseId.DECISION_POINT).canDecrease)
            vm.decrease(ExerciseId.DECISION_POINT)
            assertEquals(15, vm.row(ExerciseId.DECISION_POINT).seconds)
            assertEquals(15.seconds, repo.state.value.durations[ExerciseId.DECISION_POINT])
        }

    @Test
    fun `the maximum cannot be exceeded`() =
        runTest {
            val vm = viewModel()
            repeat(40) { vm.increase(ExerciseId.DECISION_POINT) }
            val row = vm.row(ExerciseId.DECISION_POINT)
            assertEquals(row.maxSeconds, row.seconds)
            assertFalse(row.canIncrease)
        }

    @Test
    fun `untimed can be switched on and off, including for one page`() =
        runTest {
            val vm = viewModel()
            vm.setUntimed(ExerciseId.ONE_PAGE, false)
            assertFalse(vm.row(ExerciseId.ONE_PAGE).untimed)
            vm.setUntimed(ExerciseId.CRAZY_EIGHTS, true)
            assertTrue(vm.row(ExerciseId.CRAZY_EIGHTS).untimed)
            assertEquals(480, vm.row(ExerciseId.CRAZY_EIGHTS).seconds)
        }

    @Test
    fun `switches write to the shared settings`() =
        runTest {
            val vm = viewModel()
            vm.setVibration(false)
            vm.setSound(false)
            vm.setCrazyMinuteHaptic(true)
            vm.setDailyIncludeOnePage(true)
            val saved = repo.settings.first()
            assertFalse(saved.vibrationEnabled)
            assertFalse(saved.soundEnabled)
            assertTrue(saved.crazyEightsMinuteHaptic)
            assertTrue(saved.dailyIncludeOnePage)
            assertEquals(false, vm.uiState.value.vibration)
        }

    @Test
    fun `dynamic colors are offered from API 31 only`() =
        runTest {
            assertFalse(ThemePalette.DYNAMIC in viewModel(sdkInt = 30).uiState.value.palettes)
            assertTrue(ThemePalette.DYNAMIC in viewModel(sdkInt = 31).uiState.value.palettes)
        }

    @Test
    fun `a stored dynamic palette shows as parchment below API 31`() =
        runTest {
            repo.update { it.copy(themePalette = ThemePalette.DYNAMIC) }
            assertEquals(ThemePalette.PARCHMENT, viewModel(sdkInt = 30).uiState.value.palette)
            assertEquals(ThemePalette.DYNAMIC, viewModel(sdkInt = 31).uiState.value.palette)
        }

    @Test
    fun `choosing a palette applies immediately`() =
        runTest {
            val vm = viewModel()
            vm.setPalette(ThemePalette.FOREST)
            assertEquals(ThemePalette.FOREST, repo.state.value.themePalette)
            assertEquals(ThemePalette.FOREST, vm.uiState.value.palette)
            assertNotNull(
                vm.uiState.value.palettes
                    .firstOrNull { it == ThemePalette.MIST },
            )
        }

    @Test
    fun `an unknown exercise is ignored`() =
        runTest {
            val vm = viewModel()
            vm.increase(ExerciseId("mystery"))
            assertTrue(
                repo.state.value.durations
                    .isEmpty(),
            )
        }

    @Test
    fun `vibration strength defaults to normal and is saved`() =
        runTest {
            val vm = viewModel()
            assertEquals(VibrationStrength.NORMAL, vm.uiState.value.vibrationStrength)
            vm.setVibrationStrength(VibrationStrength.SOFT)
            assertEquals(VibrationStrength.SOFT, repo.state.value.vibrationStrength)
            assertEquals(VibrationStrength.SOFT, vm.uiState.value.vibrationStrength)
        }

    @Test
    fun `trying the vibration plays the chosen strength even when vibration is switched off`() =
        runTest {
            val vm = viewModel()
            vm.setVibration(false)
            vm.setVibrationStrength(VibrationStrength.STRONG)
            vm.tryVibration()
            assertEquals(listOf(VibrationStrength.STRONG), endSignal.previews)
        }

    @Test
    fun `the state tells whether the phone can vibrate`() =
        runTest {
            assertTrue(viewModel().uiState.value.canVibrate)
        }

    @Test
    fun `theme mode defaults to system and reduce animations follows the system`() =
        runTest {
            val vm = viewModel()
            assertEquals(ThemeMode.SYSTEM, vm.uiState.value.themeMode)
            assertNull(vm.uiState.value.reduceAnimations)
            vm.setThemeMode(ThemeMode.DARK)
            vm.setReduceAnimations(true)
            assertEquals(ThemeMode.DARK, repo.state.value.themeMode)
            assertEquals(true, vm.uiState.value.reduceAnimations)
            vm.setReduceAnimations(null)
            assertNull(repo.state.value.reduceAnimations)
        }

    @Test
    fun `erasing needs a confirmation and can be cancelled`() =
        runTest {
            val vm = viewModel()
            vm.requestErase()
            assertTrue(vm.uiState.value.data.confirmingErase)
            vm.cancelErase()
            assertFalse(vm.uiState.value.data.confirmingErase)
            assertEquals(0, archive.deleteCalls)
        }

    @Test
    fun `confirming without the word does nothing`() =
        runTest {
            val vm = viewModel()
            vm.requestErase()
            vm.confirmErase()
            vm.setEraseInput("efface")
            vm.confirmErase()
            assertEquals(0, archive.deleteCalls)
            assertTrue(vm.uiState.value.data.confirmingErase)
        }

    @Test
    fun `the word is accepted whatever the case and the surrounding spaces`() =
        runTest {
            val vm = viewModel()
            vm.requestErase()
            vm.setEraseInput("  Effacer ")
            assertTrue(vm.uiState.value.data.canErase)
            vm.confirmErase()
            assertEquals(1, archive.deleteCalls)
            assertEquals(DataUiState(message = DataMessage.ERASED), vm.uiState.value.data)
        }

    @Test
    fun `cancelling forgets the typed word`() =
        runTest {
            val vm = viewModel()
            vm.requestErase()
            vm.setEraseInput("EFFACER")
            vm.cancelErase()
            vm.requestErase()
            assertEquals("", vm.uiState.value.data.eraseInput)
        }

    @Test
    fun `a failing erase is reported and keeps the settings`() =
        runTest {
            archive.failure = IOException("disk")
            val vm = viewModel()
            vm.setThemeMode(ThemeMode.DARK)
            vm.requestErase()
            vm.setEraseInput("EFFACER")
            vm.confirmErase()
            assertEquals(DataMessage.ERASE_FAILED, vm.uiState.value.data.message)
            assertEquals(ThemeMode.DARK, repo.state.value.themeMode)
        }

    @Test
    fun `the result message disappears after a while and with the next action`() =
        runTest {
            val vm = viewModel()
            vm.export(ExportLabels()) { }
            assertEquals(DataMessage.EXPORTED, vm.uiState.value.data.message)
            advanceTimeBy(SettingsViewModel.MESSAGE_MILLIS + 1)
            assertNull(vm.uiState.value.data.message)
            vm.export(ExportLabels()) { }
            vm.requestErase()
            assertNull(vm.uiState.value.data.message)
        }

    @Test
    fun `trying the vibration twice within half a second plays it once`() =
        runTest {
            val vm = viewModel()
            vm.tryVibration()
            vm.tryVibration()
            assertEquals(1, endSignal.previews.size)
            clockMillis += SettingsViewModel.TRY_DEBOUNCE_MILLIS
            vm.tryVibration()
            assertEquals(2, endSignal.previews.size)
        }

    @Test
    fun `export writes the readable text through the chosen file`() =
        runTest {
            archive.sessions = listOf(completedSession())
            var written = ""
            viewModel().export(ExportLabels(exercises = mapOf("crazy_eights" to "Crazy Eights"))) { written = it }
            assertTrue(written.contains("Crazy Eights"))
            assertTrue(written.contains("2026-10-17"))
        }

    @Test
    fun `a failing export is reported without touching the data`() =
        runTest {
            val vm = viewModel()
            vm.export(ExportLabels()) { throw IOException("full") }
            assertEquals(DataMessage.EXPORT_FAILED, vm.uiState.value.data.message)
            assertEquals(0, archive.deleteCalls)
            vm.export(ExportLabels()) { }
            assertEquals(DataMessage.EXPORTED, vm.uiState.value.data.message)
        }
}
