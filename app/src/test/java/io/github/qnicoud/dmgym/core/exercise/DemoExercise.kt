package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import io.github.qnicoud.dmgym.core.tables.TableFile
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableLoadException
import io.github.qnicoud.dmgym.core.tables.TableRepository
import io.github.qnicoud.dmgym.core.tables.TableSource
import io.github.qnicoud.dmgym.core.tables.TestTables
import io.github.qnicoud.dmgym.core.timer.TimedEvent
import kotlin.time.Duration.Companion.minutes

/** Exercice de démonstration : un slot, deux champs. Enregistré dans les tests seulement, jamais livré. */
object DemoExercise {
    val id = ExerciseId("demo")
    const val SLOT = "prompt"
    const val FIELD_A = "a"
    const val FIELD_B = "b"
    val keyA = AnswerKey(FIELD_A)
    val keyB = AnswerKey(FIELD_B)

    val definition =
        ExerciseDefinition(
            id = id,
            name = R.string.exercise_crazy_eights,
            discipline = R.string.discipline_crazy_eights,
            instructions = R.string.exercise_crazy_eights,
            reminder = null,
            timer = TimerPolicy.Required(5.minutes),
            slots = listOf(TableSlot(SLOT, TableId.Places)),
            layout = FieldLayout.Fixed(listOf(FieldSpec(FIELD_A), FieldSpec(FIELD_B))),
            progress = ProgressRule.FilledFields,
            rubric =
                listOf(
                    RubricQuestion("q1", R.string.exercise_crazy_eights),
                    RubricQuestion("q2", R.string.exercise_crazy_eights),
                ),
            acceptsFreeSubject = true,
        )

    val withEvents =
        definition.copy(
            events =
                listOf(
                    ScheduledEvent("reminder", TimedEvent.Periodic(60_000, haptic = true)),
                    ScheduledEvent("shift", TimedEvent.Once(0.4, 0.6), TableSlot("shift", TableId.Places)),
                ),
        )

    fun tables(source: FixtureSource = FixtureSource()): TableRepository = TableRepository(source)

    /** Source de test servant la table de fixture pour tous les identifiants, ou échouant à la demande. */
    class FixtureSource(
        var failing: Boolean = false,
    ) : TableSource {
        override suspend fun load(
            id: TableId,
            language: ContentLanguage,
        ): TableFile {
            if (failing) throw TableLoadException(id, IllegalStateException("broken"))
            return TableFile.decode(TestTables.resource("fixture_small.json"))
        }
    }

    fun draw(
        slot: String = SLOT,
        position: Int = 0,
        entry: String = "cave",
    ) = Draw(slot, position, "places", entry, "texte $entry")
}
