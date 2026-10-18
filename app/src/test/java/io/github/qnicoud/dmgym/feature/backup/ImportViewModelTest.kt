package io.github.qnicoud.dmgym.feature.backup

import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.core.backup.BACKUP_NOW
import io.github.qnicoud.dmgym.core.backup.BackupEncoder
import io.github.qnicoud.dmgym.core.backup.ImportError
import io.github.qnicoud.dmgym.core.backup.KNOWN_EXERCISES
import io.github.qnicoud.dmgym.core.backup.START_2026_10_17
import io.github.qnicoud.dmgym.core.backup.TooLargeException
import io.github.qnicoud.dmgym.core.backup.fullSession
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.feature.settings.FakeSessionArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Clock
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val archive = FakeSessionArchive()
    private val clock = Clock.fixed(BACKUP_NOW, ZoneOffset.UTC)
    private val a = fullSession(START_2026_10_17)
    private val b = fullSession(START_2026_10_17 + 60_000, "constraint_box")

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val handle = SavedStateHandle()

    private fun viewModel() = ImportViewModel(archive, KNOWN_EXERCISES, clock, handle, dispatcher)

    private fun fileOf(vararg sessions: SessionDetail) = BackupEncoder.encode(sessions.toList(), BACKUP_NOW, "1.1.0")

    private fun TestScope.choose(
        vm: ImportViewModel,
        text: String,
    ) {
        vm.onFileChosen { text }
        advanceUntilIdle()
    }

    @Test
    fun `starts on the choice screen`() =
        runTest(dispatcher) {
            assertEquals(ImportUiState.Choose(), viewModel().uiState.value)
        }

    @Test
    fun `a valid file shows the preview without writing anything`() =
        runTest(dispatcher) {
            archive.sessions = listOf(a.copy(session = a.session.copy(id = 1)))
            val vm = viewModel()

            choose(vm, fileOf(a, b))

            val preview = vm.uiState.value as ImportUiState.Preview
            assertEquals(1, preview.plan.toInsert.size)
            assertEquals(1, preview.plan.alreadyPresent)
            assertEquals(0, archive.importCalls)
        }

    @Test
    fun `a file whose sessions are all present has nothing to add`() =
        runTest(dispatcher) {
            archive.sessions = listOf(a)
            val vm = viewModel()

            choose(vm, fileOf(a))

            assertEquals(ImportUiState.NothingToAdd(found = 1), vm.uiState.value)
        }

    @Test
    fun `each error is typed`() =
        runTest(dispatcher) {
            val vm = viewModel()
            choose(vm, "hello")
            assertEquals(ImportUiState.Failed(ImportError.NotABackup), vm.uiState.value)

            vm.chooseAnother()
            vm.onFileChosen { throw TooLargeException(10) }
            advanceUntilIdle()
            assertEquals(ImportUiState.Failed(ImportError.TooLarge), vm.uiState.value)

            vm.chooseAnother()
            vm.onFileChosen { throw IOException("gone") }
            advanceUntilIdle()
            assertEquals(ImportUiState.Failed(ImportError.Unreadable), vm.uiState.value)

            vm.chooseAnother()
            vm.onFileChosen { throw SecurityException("revoked") }
            advanceUntilIdle()
            assertEquals(ImportUiState.Failed(ImportError.Unreadable), vm.uiState.value)
        }

    @Test
    fun `an error from the archive while previewing is reported, nothing is written`() =
        runTest(dispatcher) {
            archive.failure = IllegalStateException("db")
            val vm = viewModel()

            choose(vm, fileOf(a))

            assertEquals(ImportUiState.Failed(ImportError.Unreadable), vm.uiState.value)
        }

    @Test
    fun `the reading state only appears after 300 ms`() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.onFileChosen {
                delay(1_000)
                fileOf(a)
            }
            advanceTimeBy(299)
            assertEquals(ImportUiState.Choose(), vm.uiState.value)
            advanceTimeBy(2)
            assertEquals(ImportUiState.Reading, vm.uiState.value)
            advanceUntilIdle()
            assertInstanceOf(ImportUiState.Preview::class.java, vm.uiState.value)
        }

    @Test
    fun `a fast read never shows the reading state`() =
        runTest(dispatcher) {
            val vm = viewModel()
            val seen = mutableListOf<ImportUiState>()
            val job = backgroundScope.launch { vm.uiState.collect { seen += it } }

            choose(vm, fileOf(a))
            job.cancel()

            assertFalse(seen.contains(ImportUiState.Reading))
        }

    @Test
    fun `confirming imports and reports the result`() =
        runTest(dispatcher) {
            archive.sessions = listOf(a)
            val vm = viewModel()
            choose(vm, fileOf(a, b))

            vm.confirm()
            advanceUntilIdle()

            val done = vm.uiState.value as ImportUiState.Done
            assertEquals(1, done.added)
            assertEquals(1, done.alreadyPresent)
            assertTrue(done.canUndo)
            assertEquals(2, archive.sessions.size)
        }

    @Test
    fun `a session created between preview and confirmation is counted as present`() =
        runTest(dispatcher) {
            val vm = viewModel()
            choose(vm, fileOf(a, b))
            archive.beforeImport = { archive.sessions = listOf(a) }

            vm.confirm()
            advanceUntilIdle()

            val done = vm.uiState.value as ImportUiState.Done
            assertEquals(1, done.added)
            assertEquals(1, done.alreadyPresent)
            assertEquals(2, archive.sessions.size)
        }

    @Test
    fun `a failed write reports that nothing was added`() =
        runTest(dispatcher) {
            val vm = viewModel()
            choose(vm, fileOf(a))
            archive.failure = IllegalStateException("disk full")

            vm.confirm()
            advanceUntilIdle()

            assertEquals(ImportUiState.Failed(ImportError.WriteFailed), vm.uiState.value)
        }

    @Test
    fun `confirm outside of the preview does nothing`() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.confirm()
            advanceUntilIdle()

            assertEquals(0, archive.importCalls)
        }

    @Test
    fun `a double tap on confirm imports once`() =
        runTest(dispatcher) {
            val vm = viewModel()
            choose(vm, fileOf(a))

            vm.confirm()
            vm.confirm()
            advanceUntilIdle()

            assertEquals(1, archive.importCalls)
        }

    @Test
    fun `undo deletes exactly the imported sessions and goes back to the choice with a notice`() =
        runTest(dispatcher) {
            archive.sessions = listOf(a)
            val vm = viewModel()
            choose(vm, fileOf(a, b))
            vm.confirm()
            advanceUntilIdle()
            val importedIds = (archive.sessions - archive.sessions.first()).map { it.session.id }

            vm.undo()
            advanceUntilIdle()

            assertEquals(importedIds, archive.deletedIds)
            assertEquals(listOf(a), archive.sessions)
            assertEquals(ImportUiState.Choose(undone = true), vm.uiState.value)
        }

    @Test
    fun `a failed undo stays on the result and says so`() =
        runTest(dispatcher) {
            val vm = viewModel()
            choose(vm, fileOf(a))
            vm.confirm()
            advanceUntilIdle()
            archive.failure = IllegalStateException("db")

            vm.undo()
            advanceUntilIdle()

            val done = vm.uiState.value as ImportUiState.Done
            assertTrue(done.undoFailed)
        }

    @Test
    fun `nothing can be undone when nothing was added`() =
        runTest(dispatcher) {
            val vm = viewModel()
            choose(vm, fileOf(a))
            archive.beforeImport = { archive.sessions = listOf(a) }
            vm.confirm()
            advanceUntilIdle()

            assertFalse((vm.uiState.value as ImportUiState.Done).canUndo)
        }

    @Test
    fun `an out of memory error while reading says the file is too big`() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.onFileChosen { throw OutOfMemoryError() }
            advanceUntilIdle()

            assertEquals(ImportUiState.Failed(ImportError.TooLarge), vm.uiState.value)
        }

    @Test
    fun `the undo survives the death of the process`() =
        runTest(dispatcher) {
            archive.sessions = listOf(a)
            val first = viewModel()
            choose(first, fileOf(a, b))
            first.confirm()
            advanceUntilIdle()
            val importedIds = archive.sessions.map { it.session.id }.filter { it >= 1_000 }

            val restored = viewModel()

            assertEquals(ImportUiState.Done(added = 1, alreadyPresent = 1, canUndo = true), restored.uiState.value)
            restored.undo()
            advanceUntilIdle()
            assertEquals(importedIds, archive.deletedIds)
            assertEquals(ImportUiState.Choose(undone = true), restored.uiState.value)
            assertEquals(ImportUiState.Choose(), viewModel().uiState.value)
        }

    @Test
    fun `a double tap on undo deletes once`() =
        runTest(dispatcher) {
            val vm = viewModel()
            choose(vm, fileOf(a))
            vm.confirm()
            advanceUntilIdle()

            vm.undo()
            assertEquals(ImportUiState.Undoing, vm.uiState.value)
            vm.undo()
            advanceUntilIdle()

            assertEquals(1, archive.deleteSessionsCalls)
        }

    @Test
    fun `ignored invalid sessions reach the preview`() =
        runTest(dispatcher) {
            val bad =
                fullSession(
                    START_2026_10_17 + 5,
                ).copy().let { it.copy(session = it.session.copy(difficulty = 9)) }
            val vm = viewModel()

            choose(vm, fileOf(a, bad))

            assertEquals(1, (vm.uiState.value as ImportUiState.Preview).plan.invalid)
        }

    @Test
    fun `the notice is consumed once shown`() =
        runTest(dispatcher) {
            val vm = viewModel()
            choose(vm, fileOf(a))
            vm.confirm()
            advanceUntilIdle()
            vm.undo()
            advanceUntilIdle()

            vm.noticeShown()

            assertEquals(ImportUiState.Choose(), vm.uiState.value)
        }
}
