package io.github.qnicoud.dmgym.feature.settings

import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.backup.BackupDecoder
import io.github.qnicoud.dmgym.core.backup.DecodeResult
import io.github.qnicoud.dmgym.feature.exercise.FakeEndSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsBackupTest {
    private val archive = FakeSessionArchive()
    private val clock = Clock.fixed(Instant.parse("2026-10-18T14:03:00Z"), ZoneOffset.UTC)

    private fun viewModel() =
        SettingsViewModel(
            FakeSettingsRepository(),
            34,
            FakeEndSignal(),
            archive,
            clock,
            UnconfinedTestDispatcher(),
        )

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `backup writes a re-importable json and reports the file name and session count`() =
        runTest {
            archive.sessions = listOf(completedSession())
            var written = ""
            val vm = viewModel()

            vm.backup("1.1.0") {
                written = it
                "ma-sauvegarde.json"
            }

            val data = vm.uiState.value.data
            assertEquals(DataMessage.BACKED_UP, data.message)
            assertEquals(BackupInfo("ma-sauvegarde.json", 1), data.backup)
            val decoded =
                BackupDecoder.decode(written, setOf("crazy_eights"), Instant.parse("2026-10-18T14:03:00Z"))
            assertEquals(1, (decoded as DecodeResult.Success).sessions.size)
        }

    @Test
    fun `the count only includes sessions that are exported`() =
        runTest {
            archive.sessions = listOf(completedSession())
            val vm = viewModel()

            vm.backup("1.1.0") { "x.json" }

            assertEquals(
                1,
                vm.uiState.value.data.backup
                    ?.sessionCount,
            )
        }

    @Test
    fun `a failing backup is reported without a file name`() =
        runTest {
            archive.sessions = listOf(completedSession())
            val vm = viewModel()

            vm.backup("1.1.0") { throw IOException("full") }

            assertEquals(DataMessage.BACKUP_FAILED, vm.uiState.value.data.message)
            assertNull(vm.uiState.value.data.backup)
        }

    @Test
    fun `a failing read of the sessions is reported`() =
        runTest {
            archive.failure = IOException("db")
            val vm = viewModel()

            vm.backup("1.1.0") { "x.json" }

            assertEquals(DataMessage.BACKUP_FAILED, vm.uiState.value.data.message)
        }
}
