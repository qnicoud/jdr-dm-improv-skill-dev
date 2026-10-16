package io.github.qnicoud.dmgym.core.tables

import java.io.File

/** Test helpers: fixtures from `src/test/resources/tables/` and real tables from the main assets. */
object TestTables {
    fun fixtureSmall(): TableFile = TableFile.decode(resource("fixture_small.json"))

    fun resource(name: String): String =
        checkNotNull(TestTables::class.java.getResource("/tables/$name")) { "Missing fixture $name" }.readText()

    fun assetsDir(): File = File("src/main/assets/tables")

    fun assetFile(id: TableId): File = File(assetsDir(), "${id.fileName}.json")
}
