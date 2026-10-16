package io.github.qnicoud.dmgym.data.smoke

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmokeDatabaseTest {
    private lateinit var database: DmGymDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder<DmGymDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertedEntityCanBeReadBack() =
        runTest {
            val dao = database.smokeDao()

            val id = dao.insert(SmokeEntity(label = "hello"))

            assertEquals(SmokeEntity(id = id, label = "hello"), dao.findById(id))
        }

    @Test
    fun missingEntityIsNull() =
        runTest {
            assertNull(database.smokeDao().findById(42))
        }
}
