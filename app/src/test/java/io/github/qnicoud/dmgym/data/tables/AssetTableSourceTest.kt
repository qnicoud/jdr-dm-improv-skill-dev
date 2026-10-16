package io.github.qnicoud.dmgym.data.tables

import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableLoadException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AssetTableSourceTest {
    private val assets = ApplicationProvider.getApplicationContext<android.content.Context>().assets

    @Test
    fun `loads and decodes a shipped table`() =
        runBlocking {
            val table = AssetTableSource(assets, Dispatchers.IO).load(TableId.CoreImages)

            assertEquals("core_images", table.table)
            assertTrue(table.entries.size >= 30)
        }

    @Test
    fun `loads every shipped table`() =
        runBlocking {
            val source = AssetTableSource(assets, Dispatchers.Unconfined)

            TableId.entries.forEach { assertEquals(it.fileName, source.load(it).table) }
        }

    @Test
    fun `missing file is reported as a TableLoadException`() =
        runBlocking {
            val source = AssetTableSource(assets, Dispatchers.Unconfined, basePath = "nowhere")

            try {
                source.load(TableId.Places)
                fail("expected TableLoadException")
            } catch (e: TableLoadException) {
                assertEquals(TableId.Places, e.id)
            }
        }
}
