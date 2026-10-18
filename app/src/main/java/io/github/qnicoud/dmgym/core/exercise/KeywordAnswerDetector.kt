package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.detection.CompiledTargets
import io.github.qnicoud.dmgym.core.detection.DetectionTarget
import io.github.qnicoud.dmgym.core.detection.KeywordDetector
import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableLoadException
import io.github.qnicoud.dmgym.core.tables.TableRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * [AnswerDetector] branché sur les `keywords` des tables (ADR 0008). [prepare] lit les mots-clés des tirages ;
 * [detect] et [highlights] sont ensuite synchrones et réutilisent des cibles compilées une seule fois par tirage.
 */
class KeywordAnswerDetector(
    private val tables: TableRepository,
    private val language: ContentLanguage = ContentLanguage.DEFAULT,
    private val engine: KeywordDetector = KeywordDetector(),
    /** Appelé quand une table est illisible : la détection reste muette, le cochage manuel continue de marcher. */
    private val onFailure: (TableLoadException) -> Unit = {},
) : AnswerDetector {
    private val lock = Any()
    private val siblings = ConcurrentHashMap<ContentLanguage, KeywordAnswerDetector>()
    private val targets = mutableMapOf<String, DetectionTarget>()
    private var compiled: Pair<List<String>, CompiledTargets>? = null

    /** Un détecteur par langue, qui garde ses propres cibles : un mot-clé français n'est jamais lu en anglais. */
    override fun forLanguage(language: ContentLanguage): AnswerDetector =
        if (language == this.language) {
            this
        } else {
            siblings.getOrPut(language) { KeywordAnswerDetector(tables, language, engine, onFailure) }
        }

    override suspend fun prepare(draws: List<Draw>) {
        draws
            .mapNotNull { it.tableId }
            .distinct()
            .forEach { loadTable(it) }
    }

    private suspend fun loadTable(fileName: String) {
        val id = TableId.entries.firstOrNull { it.fileName == fileName } ?: return
        val file =
            try {
                tables.table(id, language)
            } catch (e: TableLoadException) {
                onFailure(e)
                return
            }
        synchronized(lock) {
            file.entries.forEach {
                val key = keyOf(fileName, it.id)
                targets[key] = DetectionTarget(key, it.keywords, it.fuzzy)
            }
        }
    }

    override fun detect(
        draws: List<Draw>,
        text: String,
    ): Map<DrawKey, Boolean> {
        val known = draws.filter { keyOf(it) in targets }
        if (known.isEmpty()) return emptyMap()
        val result = engine.detect(text, compiledFor(known))
        return known.associate { DrawKey(it.slotKey, it.position) to result.isDetected(keyOf(it)) }
    }

    override fun occurrences(
        draws: List<Draw>,
        text: String,
    ): Map<DrawKey, Int> {
        val known = draws.filter { keyOf(it) in targets }
        if (known.isEmpty()) return emptyMap()
        val matches = engine.detect(text, compiledFor(known)).matches
        return known.associate { DrawKey(it.slotKey, it.position) to matches[keyOf(it)].orEmpty().size }
    }

    override fun highlights(
        draws: List<Draw>,
        text: String,
    ): List<IntRange> {
        val known = draws.filter { keyOf(it) in targets }
        if (known.isEmpty() || text.isEmpty()) return emptyList()
        val matches = engine.detect(text, compiledFor(known)).matches
        return matches.values
            .flatten()
            .sortedWith(compareBy({ it.first }, { it.last }))
            .let(::merge)
    }

    private fun compiledFor(draws: List<Draw>): CompiledTargets =
        synchronized(lock) {
            val ids = draws.map(::keyOf)
            compiled?.takeIf { it.first == ids }?.second
                ?: engine.compile(ids.mapNotNull { targets[it] }).also { compiled = ids to it }
        }

    private fun merge(sorted: List<IntRange>): List<IntRange> =
        sorted.fold(emptyList<IntRange>()) { acc, range ->
            val last = acc.lastOrNull()
            if (last != null && range.first <= last.last) {
                acc.dropLast(1) + listOf(last.first..maxOf(last.last, range.last))
            } else {
                acc + listOf(range)
            }
        }

    private fun keyOf(draw: Draw): String = keyOf(draw.tableId.orEmpty(), draw.entryId)

    private fun keyOf(
        table: String,
        entry: String,
    ) = "$table/$entry"
}
