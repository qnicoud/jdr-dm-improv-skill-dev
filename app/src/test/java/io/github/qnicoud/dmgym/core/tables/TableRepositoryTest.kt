package io.github.qnicoud.dmgym.core.tables

import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TableRepositoryTest {
    private class CountingSource(
        private val file: TableFile,
        private var failures: Int = 0,
    ) : TableSource {
        val loads = mutableListOf<TableId>()
        val languages = mutableListOf<ContentLanguage>()

        override suspend fun load(
            id: TableId,
            language: ContentLanguage,
        ): TableFile {
            loads += id
            languages += language
            if (failures > 0) {
                failures--
                throw TableLoadException(id, IllegalStateException("boom"))
            }
            return file
        }
    }

    @Test
    fun `loads a table only once`() =
        runTest {
            val source = CountingSource(TestTables.fixtureSmall())
            val repository = TableRepository(source)

            val first = repository.table(TableId.Places)
            val second = repository.table(TableId.Places)

            assertSame(first, second)
            assertEquals(listOf(TableId.Places), source.loads)
        }

    @Test
    fun `concurrent requests trigger a single load`() =
        runTest {
            val source = CountingSource(TestTables.fixtureSmall())
            val repository = TableRepository(source)

            List(5) { async { repository.table(TableId.Problems) } }.awaitAll()

            assertEquals(1, source.loads.size)
        }

    @Test
    fun `each table is loaded separately`() =
        runTest {
            val source = CountingSource(TestTables.fixtureSmall())
            val repository = TableRepository(source)

            repository.table(TableId.Places)
            repository.table(TableId.Problems)

            assertEquals(listOf(TableId.Places, TableId.Problems), source.loads)
        }

    @Test
    fun `a failed load is not cached`() =
        runTest {
            val source = CountingSource(TestTables.fixtureSmall(), failures = 1)
            val repository = TableRepository(source)

            val failure = runCatching { repository.table(TableId.Places) }.exceptionOrNull()
            repository.table(TableId.Places)

            assertTrue(failure is TableLoadException)

            assertEquals(2, source.loads.size)
        }

    @Test
    fun `asks the source for the requested language and defaults to french`() =
        runTest {
            val source = CountingSource(TestTables.fixtureSmall())
            val repository = TableRepository(source)

            repository.table(TableId.Places)
            repository.table(TableId.Places, ContentLanguage.FR)

            assertEquals(listOf(ContentLanguage.FR), source.languages)
        }

    @Test
    fun `asset path is tables slash language slash file`() {
        assertEquals("tables/fr/problems.json", TableId.Problems.assetPath(ContentLanguage.FR))
        assertEquals("x/fr/problems.json", TableId.Problems.assetPath(ContentLanguage.FR, "x"))
    }
}
