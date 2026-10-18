package io.github.qnicoud.dmgym.core.tables

import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import java.io.File

/** Test helpers: fixtures from `src/test/resources/tables/` and real tables from the main assets. */
object TestTables {
    fun fixtureSmall(): TableFile = TableFile.decode(resource("fixture_small.json"))

    fun resource(name: String): String =
        checkNotNull(TestTables::class.java.getResource("/tables/$name")) { "Missing fixture $name" }.readText()

    fun assetsDir(language: ContentLanguage = ContentLanguage.DEFAULT): File =
        File("src/main/assets/tables/${language.code}")

    fun assetFile(
        id: TableId,
        language: ContentLanguage = ContentLanguage.DEFAULT,
    ): File = File(assetsDir(language), "${id.fileName}.json")
}
