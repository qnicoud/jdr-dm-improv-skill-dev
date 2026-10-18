package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.DemoEngine
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.exercise.unknownHintCodes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Cohérence entre un champ lié ([FieldLayout.Fixed.boundSlot]) et la table de son slot. `init` n'a pas accès aux
 * tables : le test le fait pour chaque définition livrée (les futures définitions y entrent d'elles-mêmes).
 */
class BoundFieldsContentTest {
    private val shipped = ExerciseRegistry.Default.all

    private fun ExerciseDefinition.boundTable(): Pair<FieldLayout.Fixed, TableSlot>? {
        val layout = layout as? FieldLayout.Fixed ?: return null
        val slot = slots.firstOrNull { it.key == layout.boundSlot } as? TableSlot ?: return null
        return layout to slot
    }

    @Test
    fun `every hint code of a shipped definition exists in its table`() {
        (shipped + DemoEngine.bound).forEach { definition ->
            val (layout, slot) = definition.boundTable() ?: return@forEach
            val unknown = layout.unknownHintCodes(RealTables.file(slot.table))
            assertEquals(emptySet<String>(), unknown, "${definition.id}: unknown hint codes")
        }
    }

    @Test
    fun `an unknown hint code is reported`() {
        val slot = DemoEngine.bound.boundTable()!!.second
        val layout = FieldLayout.Fixed(listOf(FieldSpec("a")), boundSlot = slot.key, hintByCode = mapOf("nope" to 1))
        assertEquals(setOf("nope"), layout.unknownHintCodes(RealTables.file(slot.table)))
    }

    @Test
    fun `every entry of a linked table has a field icon`() {
        shipped.forEach { definition ->
            val (_, slot) = definition.boundTable() ?: return@forEach
            RealTables.file(slot.table).entries.forEach {
                assertNotNull(fieldIcon(it.id), "${definition.id}: no icon for '${it.id}'")
            }
        }
    }
}
