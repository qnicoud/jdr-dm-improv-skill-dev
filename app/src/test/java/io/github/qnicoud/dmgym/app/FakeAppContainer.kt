package io.github.qnicoud.dmgym.app

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.ExerciseCountRecord
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.feature.home.ExerciseCatalog
import io.github.qnicoud.dmgym.feature.home.SettingsExerciseCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeSettingsRepository(
    initial: Settings = Settings(untimed = ExerciseId.UNTIMED_BY_DEFAULT),
) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<Settings> = state

    override suspend fun update(transform: (Settings) -> Settings) {
        state.value = transform(state.value)
    }
}

/** Seul `observeSessionCount` est utilisé par l'accueil ; le reste échoue explicitement. */
class FakeHistoryRepository(
    val sessionCount: Flow<Int> = MutableStateFlow(0),
) : HistoryRepository {
    override fun observeSessionCount(): Flow<Int> = sessionCount

    override suspend fun createSession(session: SessionRecord): Long = unused()

    override suspend fun updateSession(session: SessionRecord) = unused()

    override suspend fun saveSnapshot(
        session: SessionRecord,
        draws: List<DrawRecord>,
        answers: List<AnswerRecord>,
        rubric: List<RubricRecord>,
    ) = unused()

    override suspend fun deleteSession(id: Long) = unused()

    override suspend fun upsertDraws(
        sessionId: Long,
        draws: List<DrawRecord>,
    ) = unused()

    override suspend fun upsertAnswers(
        sessionId: Long,
        answers: List<AnswerRecord>,
    ) = unused()

    override suspend fun upsertRubric(
        sessionId: Long,
        rubric: List<RubricRecord>,
    ) = unused()

    override fun observeHistory(
        exerciseId: String?,
        gemsOnly: Boolean,
    ): Flow<List<SessionRecord>> = flowOf(emptyList())

    override fun observeDetail(id: Long): Flow<SessionDetail?> = flowOf(null)

    override fun observePracticedDays(): Flow<List<Long>> = flowOf(emptyList())

    override suspend fun countSessionsByExercise(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<ExerciseCountRecord> = unused()

    override suspend fun purgeEmptyInProgress(): Int = unused()

    private fun unused(): Nothing = error("not used by these tests")
}

/** Conteneur de test : aucune dépendance Android ni stockage. */
class FakeAppContainer(
    override val settingsRepository: SettingsRepository = FakeSettingsRepository(),
    override val historyRepository: HistoryRepository = FakeHistoryRepository(),
) : AppContainer {
    override val exerciseCatalog: ExerciseCatalog = SettingsExerciseCatalog(settingsRepository)
}
