package io.github.qnicoud.dmgym.feature.settings

import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val repo = FakeSettingsRepository()

    private fun viewModel(sdkInt: Int = 34) = SettingsViewModel(repo, sdkInt)

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
}
