package io.github.qnicoud.dmgym.data.history

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.data.history.db.DmGymDatabase
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
 * Migration 1 → 2 (colonne `session_draws.category`). La base de départ est construite à partir du schéma
 * exporté `1.json`, tel que la v1 de l'app l'a créée, puis ouverte par Room à la version courante.
 */
@RunWith(RobolectricTestRunner::class)
class RoomMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file = File(context.cacheDir, "migration-test.db")

    private fun schemaV1(): JSONObject {
        val dir = File("schemas/io.github.qnicoud.dmgym.data.history.db.DmGymDatabase")
        return JSONObject(File(dir, "1.json").readText()).getJSONObject("database")
    }

    private fun createV1WithData() {
        file.delete()
        val schema = schemaV1()
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

    private fun open(): DmGymDatabase =
        Room
            .databaseBuilder<DmGymDatabase>(context, file.absolutePath)
            .setDriver(AndroidSQLiteDriver())
            .build()

    @Test
    fun `migrating from 1 to 2 keeps every row and gives old draws no category`() =
        runTest {
            createV1WithData()
            val database = open()
            val detail = checkNotNull(database.sessionDao().observeDetail(1).first())
            assertEquals("constraint_box", detail.session.exerciseId)
            assertEquals(2, detail.session.selfRating)
            assertEquals(true, detail.session.isGem)
            assertEquals(listOf("brumeux", "cave"), detail.draws.map { it.entryId }.sorted())
            assertEquals("une cave", detail.draws.first { it.entryId == "cave" }.textSnapshot)
            assertEquals(true, detail.draws.first { it.entryId == "brumeux" }.autoDetected)
            assertEquals(1, detail.answers.size)
            assertEquals("Il fait brumeux.", detail.answers.single().text)
            assertEquals(1, detail.rubric.size)
            detail.draws.forEach { assertNull(it.category) }
            database.close()
        }

    @Test
    fun `the migrated database is at version 2`() =
        runTest {
            createV1WithData()
            val database = open()
            database.sessionDao().observeDetail(1).first()
            database.close()
            val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val version = db.version
            db.close()
            assertEquals(2, version)
        }
}
