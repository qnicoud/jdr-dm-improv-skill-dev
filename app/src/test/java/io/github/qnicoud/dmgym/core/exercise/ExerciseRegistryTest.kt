package io.github.qnicoud.dmgym.core.exercise

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ExerciseRegistryTest {
    @Test
    fun `finds a registered definition`() {
        val registry = ExerciseRegistry(listOf(DemoExercise.definition))
        assertEquals(DemoExercise.definition, registry[DemoExercise.id])
        assertEquals(listOf(DemoExercise.definition), registry.all)
    }

    @Test
    fun `unknown exercise gives null`() {
        assertNull(ExerciseRegistry(emptyList())[ExerciseId.CRAZY_EIGHTS])
    }

    @Test
    fun `the shipped registry has the L8 exercises, each under its own id`() {
        listOf(ExerciseId.CRAZY_EIGHTS, ExerciseId.CONSTRAINT_BOX, ExerciseId.CONNECT_STORY).forEach {
            assertEquals(it, ExerciseRegistry.Default[it]?.id)
        }
    }

    @Test
    fun `rejects duplicate definitions`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExerciseRegistry(listOf(DemoExercise.definition, DemoExercise.definition))
        }
    }
}
