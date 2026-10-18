package io.github.qnicoud.dmgym.data.history

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.core.backup.BACKUP_NOW
import io.github.qnicoud.dmgym.core.backup.BackupDecoder
import io.github.qnicoud.dmgym.core.backup.BackupEncoder
import io.github.qnicoud.dmgym.core.backup.DecodeResult
import io.github.qnicoud.dmgym.core.backup.EPOCH_DAY_2026_10_17
import io.github.qnicoud.dmgym.core.backup.ImportPlanner
import io.github.qnicoud.dmgym.core.backup.KNOWN_EXERCISES
import io.github.qnicoud.dmgym.core.backup.fullSession
import io.github.qnicoud.dmgym.core.backup.inProgressWithoutContent
import io.github.qnicoud.dmgym.core.backup.withSession
import io.github.qnicoud.dmgym.core.backup.withoutIds
import io.github.qnicoud.dmgym.core.history.SessionArchive
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.data.history.db.DmGymDatabase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Scénario de passage à l'APK signé (docs/qa/release.md) : base v3 → sauvegarde → base vide → import. */
@RunWith(RobolectricTestRunner::class)
class BackupRoundTripTest {
    private companion object {
        const val DAY = 86_400_000L
    }

    private val databases = mutableListOf<DmGymDatabase>()

    @Before
    fun setUp() = databases.clear()

    @After
    fun tearDown() = databases.forEach { it.close() }

    private fun newArchive(): SessionArchive {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder<DmGymDatabase>(context).setDriver(AndroidSQLiteDriver()).build()
        databases += db
        val ticks = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }
        return RoomHistoryRepository(
            db.sessionDao(),
            Clock.fixed(Instant.parse("2026-10-18T08:00:00Z"), ZoneOffset.UTC),
            ticks,
            checkpoint = {},
        )
    }

    @Test
    fun `backup then import into an empty database restores identical sessions`() =
        runTest {
            val source = newArchive()
            val sessions =
                listOf(
                    fullSession(startedAt = 1_792_220_400_000L - 2 * DAY).onDay(-2),
                    fullSession(startedAt = 1_792_220_400_000L - DAY, exerciseId = "core_image").onDay(-1),
                    fullSession(startedAt = 1_792_220_400_000L, exerciseId = "constraint_box").onDay(0),
                )
            source.importSessions(sessions + inProgressWithoutContent())
            val expected = BackupEncoder.exportable(source.readAll()).withoutIds()
            assertEquals(3, expected.size)

            val file = BackupEncoder.encode(source.readAll(), BACKUP_NOW, "1.1.0")

            val target = newArchive()
            assertTrue(target.readAll().isEmpty())
            val decoded = BackupDecoder.decode(file, KNOWN_EXERCISES, BACKUP_NOW) as DecodeResult.Success
            val plan =
                ImportPlanner.plan(
                    decoded.sessions,
                    decoded.unknownExercise,
                    target.identities(),
                    decoded.invalid,
                )
            assertEquals(3, plan.found)
            target.importSessions(plan.toInsert)

            assertEquals(expected, target.readAll().withoutIds())
        }

    private fun SessionDetail.onDay(offset: Long) =
        withSession { it.copy(localEpochDay = EPOCH_DAY_2026_10_17 + offset) }

    @Test
    fun `importing the same backup twice adds nothing`() =
        runTest {
            val source = newArchive()
            source.importSessions(listOf(fullSession()))
            val file = BackupEncoder.encode(source.readAll(), BACKUP_NOW, "1.1.0")
            val decoded = BackupDecoder.decode(file, KNOWN_EXERCISES, BACKUP_NOW) as DecodeResult.Success

            val plan =
                ImportPlanner.plan(
                    decoded.sessions,
                    decoded.unknownExercise,
                    source.identities(),
                    decoded.invalid,
                )

            assertTrue(plan.toInsert.isEmpty())
            assertEquals(1, plan.alreadyPresent)
        }
}
