package io.github.qnicoud.dmgym.ui.components

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/** Un identifiant inconnu (session d'une version plus récente, import) ne doit jamais faire planter l'affichage. */
class ExerciseLabelsTest {
    private val unknown = ExerciseId("from_the_future")

    @Test
    fun `an unknown exercise has a neutral name and discipline`() {
        assertEquals(R.string.exercise_unknown, unknown.nameRes)
        assertEquals(R.string.discipline_unknown, unknown.disciplineRes)
    }

    @Test
    fun `an unknown exercise has a generic icon`() {
        assertNotNull(DmIcons.forExercise(unknown))
    }

    @Test
    fun `every known exercise keeps its own name`() {
        assertEquals(R.string.exercise_crazy_eights, ExerciseId.CRAZY_EIGHTS.nameRes)
        ExerciseId.ALL.forEach { assertNotNull(DmIcons.forExercise(it)) }
    }
}
