package io.github.qnicoud.dmgym.data.history

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.data.history.db.DmGymDatabase
import io.github.qnicoud.dmgym.data.history.db.DrawEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Migrations 1 → 2 (colonne `session_draws.category`) et 2 → 3 (colonne `sessions.contentLocale`, ADR 0016). La base
 * de départ est construite à partir du schéma exporté (`1.json` ou `2.json`), tel que l'app l'a créée, puis ouverte
 * par Room à la version courante.
 */
@RunWith(RobolectricTestRunner::class)
class RoomMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file = File(context.cacheDir, "migration-test.db")

    private fun schema(version: Int): JSONObject {
        val dir = File("schemas/io.github.qnicoud.dmgym.data.history.db.DmGymDatabase")
        return JSONObject(File(dir, "$version.json").readText()).getJSONObject("database")
    }

    /** Crée la base vide d'un schéma exporté et la laisse ouverte pour que l'appelant y insère ses lignes. */
    private fun createEmpty(version: Int): SQLiteDatabase {
        file.delete()
        val schema = schema(version)
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices")
            for (j in 0 until (indices?.length() ?: 0)) {
                db.execSQL(indices!!.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        return db
    }

    private fun createV1WithData() {
        val db = createEmpty(1)
        db.execSQL(
            "INSERT INTO sessions VALUES " +
                "(1, 'constraint_box', 'COMPLETED', 1000, 2000, 3000, 'UTC', 100, 300, 120, 0, 1, '{}', 2, 1, 1, 42)",
        )
        db.execSQL("INSERT INTO session_draws VALUES (1, 'words', 0, 'sensory_words', 'brumeux', 'brumeux', 1, NULL)")
        db.execSQL("INSERT INTO session_draws VALUES (1, 'place', 0, 'places', 'cave', 'une cave', NULL, NULL)")
        db.execSQL("INSERT INTO session_answers VALUES (1, 'text', 0, 'Il fait brumeux.', 0, 1)")
        db.execSQL("INSERT INTO session_rubric VALUES (1, 'say_it_aloud', 1)")
        db.version = 1
        db.close()
    }

    private fun createV2WithData() {
        val db = createEmpty(2)
        db.execSQL(
            "INSERT INTO sessions VALUES " +
                "(7, 'one_page', 'IN_PROGRESS', 5000, 6000, NULL, 'Europe/Paris', 200, NULL, 60, 0, 2, '{}', NULL, 0, 0, 9)",
        )
        db.execSQL("INSERT INTO session_draws VALUES (7, 'place', 0, 'places', 'cave', 'une cave', NULL, NULL, 'x')")
        db.execSQL("INSERT INTO session_answers VALUES (7, 'text', 0, 'Brouillon', 0, 0)")
        db.execSQL("INSERT INTO session_rubric VALUES (7, 'say_it_aloud', 0)")
        db.version = 2
        db.close()
    }

    private fun open(): DmGymDatabase =
        Room
            .databaseBuilder<DmGymDatabase>(context, file.absolutePath)
            .setDriver(AndroidSQLiteDriver())
            .build()

    private fun storedVersion(): Int {
        val raw = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val version = raw.version
        raw.close()
        return version
    }

    @Test
    fun `migrating from 1 to 3 keeps every row, gives old draws no category and accepts new ones`() =
        runTest {
            createV1WithData()
            val database = open()
            val dao = database.sessionDao()
            val detail = checkNotNull(dao.observeDetail(1).first())
            assertEquals("constraint_box", detail.session.exerciseId)
            assertEquals(2, detail.session.selfRating)
            assertEquals(true, detail.session.isGem)
            assertEquals("fr", detail.session.contentLocale)
            assertEquals(listOf("brumeux", "cave"), detail.draws.map { it.entryId }.sorted())
            assertEquals("une cave", detail.draws.first { it.entryId == "cave" }.textSnapshot)
            assertEquals(true, detail.draws.first { it.entryId == "brumeux" }.autoDetected)
            assertEquals(1, detail.answers.size)
            assertEquals("Il fait brumeux.", detail.answers.single().text)
            assertEquals(1, detail.rubric.size)
            detail.draws.forEach { assertNull(it.category) }

            dao.upsertDraws(
                listOf(DrawEntity(1, "words", 1, "sensory_words", "rouille", "rouillé", null, null, "sight")),
            )
            val after = checkNotNull(dao.observeDetail(1).first())
            assertEquals(3, after.draws.size)
            assertEquals("sight", after.draws.first { it.entryId == "rouille" }.category)
            assertNull(after.draws.first { it.entryId == "cave" }.category)
            database.close()

            assertEquals(3, storedVersion())
        }

    @Test
    fun `migrating from 2 to 3 keeps the data and defaults the language of existing sessions to fr`() =
        runTest {
            createV2WithData()
            val database = open()
            val detail = checkNotNull(database.sessionDao().observeDetail(7).first())

            assertEquals("one_page", detail.session.exerciseId)
            assertEquals("IN_PROGRESS", detail.session.status)
            assertEquals(9L, detail.session.drawSeed)
            assertNull(detail.session.selfRating)
            assertEquals("fr", detail.session.contentLocale)
            assertEquals("x", detail.draws.single().category)
            assertEquals("Brouillon", detail.answers.single().text)
            assertEquals(false, detail.rubric.single().value)
            database.close()

            assertEquals(3, storedVersion())
        }

    @Test
    fun `a session written after the migration keeps its own language`() =
        runTest {
            createV2WithData()
            val database = open()
            val dao = database.sessionDao()
            val old = checkNotNull(dao.observeDetail(7).first()).session

            val id = dao.insert(old.copy(id = 0, contentLocale = "en"))

            assertEquals("en", checkNotNull(dao.observeDetail(id).first()).session.contentLocale)
            assertEquals("fr", checkNotNull(dao.observeDetail(7).first()).session.contentLocale)
            database.close()
        }
}
