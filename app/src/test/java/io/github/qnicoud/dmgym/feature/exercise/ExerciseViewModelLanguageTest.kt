package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** La langue d'une séance est fixée avant le premier tirage et enregistrée avec elle (ADR 0016 §3). */
class ExerciseViewModelLanguageTest : ExerciseViewModelTestBase() {
    @Test
    fun `a new session is recorded with the content language and keeps it in the handle`() =
        runTest {
            started()

            assertEquals(listOf(ContentLanguage.FR.code), history.sessions.values.map { it.session.contentLocale })
            assertEquals(ContentLanguage.FR.code, handle.get<String>(HandleKeys.LANGUAGE))
        }

    @Test
    fun `the language kept in the handle wins over the current language of the app`() =
        runTest {
            handle.set(HandleKeys.LANGUAGE, ContentLanguage.FR.code)

            val vm = vm()
            collect(vm)
            runCurrent()

            assertEquals(ContentLanguage.FR, vm.sessionLanguage)
        }

    @Test
    fun `a resumed session comes back in the language it was drawn in`() =
        runTest {
            history.sessions[5] =
                SessionDetail(
                    SessionRecord(
                        id = 5,
                        exerciseId = DemoExercise.id.value,
                        status = SessionStatus.IN_PROGRESS,
                        startedAtEpochMillis = 0,
                        zoneId = "UTC",
                        localEpochDay = 0,
                        contentLocale = "fr",
                    ),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                )

            val vm = vm(launch = ExerciseLaunch(resumeSessionId = 5, seed = 7))
            collect(vm)
            runCurrent()

            assertEquals(ContentLanguage.FR, vm.sessionLanguage)
        }
}
