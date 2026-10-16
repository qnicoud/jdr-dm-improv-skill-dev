package io.github.qnicoud.dmgym.data.tables

import android.content.res.AssetManager
import io.github.qnicoud.dmgym.core.tables.TableFile
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableLoadException
import io.github.qnicoud.dmgym.core.tables.TableSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Reads `assets/<basePath>/<name>.json` and decodes it strictly (ADR 0009). */
class AssetTableSource(
    private val assets: AssetManager,
    private val ioDispatcher: CoroutineDispatcher,
    private val basePath: String = "tables",
) : TableSource {
    override suspend fun load(id: TableId): TableFile =
        withContext(ioDispatcher) {
            try {
                val text = assets.open("$basePath/${id.fileName}.json").bufferedReader().use { it.readText() }
                TableFile.decode(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw TableLoadException(id, e)
            }
        }
}
