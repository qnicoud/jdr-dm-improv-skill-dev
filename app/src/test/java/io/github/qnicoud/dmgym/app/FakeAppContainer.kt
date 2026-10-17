package io.github.qnicoud.dmgym.app

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.ExerciseCountRecord
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionArchive
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.signal.EndSignal
import io.github.qnicoud.dmgym.core.tables.TableRepository
import io.github.qnicoud.dmgym.feature.exercise.ExerciseDependencies
import io.github.qnicoud.dmgym.feature.exercise.FakeEndSignal
import io.github.qnicoud.dmgym.feature.home.ExerciseCatalog
import io.github.qnicoud.dmgym.feature.home.SettingsExerciseCatalog
import io.github.qnicoud.dmgym.feature.settings.FakeSessionArchive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FakeSettingsRepository(
    initial: Settings = Settings(untimed = ExerciseId.UNTIMED_BY_DEFAULT),
) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<Settings> = state

    /** Nombre d'écritures reçues. */
    var updates = 0
        private set

    override suspend fun update(transform: (Settings) -> Settings) {
        updates++
        state.value = transform(state.value)
    }
}

/**
 * Lectures configurables (compteur, jours, historique, comptages, détails, aperçus, session à reprendre) ;
 * seules la mise à jour d'une session et sa suppression sont gérées en mémoire, le reste échoue explicitement.
 */
class FakeHistoryRepository(
    val sessionCount: Flow<Int> = MutableStateFlow(0),
    val history: Flow<List<SessionRecord>> = MutableStateFlow(emptyList()),
    val practicedDays: Flow<List<Long>> = MutableStateFlow(emptyList()),
    val counts: (Long, Long) -> List<ExerciseCountRecord> = { _, _ -> emptyList() },
    val resumable: Flow<SessionDetail?> = MutableStateFlow(null),
    val previews: Flow<Map<Long, String>> = MutableStateFlow(emptyMap()),
    val details: MutableStateFlow<Map<Long, SessionDetail>> = MutableStateFlow(emptyMap()),
    val historyBy: (String?, Boolean) -> Flow<List<SessionRecord>> = { _, _ -> history },
) : HistoryRepository {
    /** Filtres demandés à `observeHistory`, dans l'ordre. */
    val historyCalls = mutableListOf<Pair<String?, Boolean>>()
    val ratings = mutableListOf<Pair<Long, Int?>>()
    val gems = mutableListOf<Pair<Long, Boolean>>()
    val deleted = mutableListOf<Long>()
    var failOnUpdate = false
    var failOnDelete = false

    /** Fenêtres demandées à `countSessionsByExercise`, dans l'ordre. */
    val countCalls = mutableListOf<Pair<Long, Long>>()

    override fun observeSessionCount(): Flow<Int> = sessionCount

    override suspend fun createSession(session: SessionRecord): Long = unused()

    override suspend fun updateSession(session: SessionRecord) = unused()

    override suspend fun updateRating(
        sessionId: Long,
        rating: Int?,
    ) {
        check(!failOnUpdate) { "disk full" }
        ratings += sessionId to rating
        patch(sessionId) { it.copy(selfRating = rating) }
    }

    override suspend fun updateGem(
        sessionId: Long,
        isGem: Boolean,
    ) {
        check(!failOnUpdate) { "disk full" }
        gems += sessionId to isGem
        patch(sessionId) { it.copy(isGem = isGem) }
    }

    private fun patch(
        id: Long,
        change: (SessionRecord) -> SessionRecord,
    ) {
        details.value =
            details.value.mapValues { (key, d) -> if (key == id) d.copy(session = change(d.session)) else d }
    }

    override fun observeResumable(): Flow<SessionDetail?> = resumable

    override fun observePreviews(): Flow<Map<Long, String>> = previews

    override suspend fun saveSnapshot(
        session: SessionRecord,
        draws: List<DrawRecord>,
        answers: List<AnswerRecord>,
        rubric: List<RubricRecord>,
    ) = unused()

    override suspend fun deleteSession(id: Long) {
        check(!failOnDelete) { "disk full" }
        deleted += id
        details.value = details.value - id
    }

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
    ): Flow<List<SessionRecord>> {
        historyCalls += exerciseId to gemsOnly
        return historyBy(exerciseId, gemsOnly)
    }

    override fun observeDetail(id: Long): Flow<SessionDetail?> = details.map { it[id] }

    override fun observePracticedDays(): Flow<List<Long>> = practicedDays

    override suspend fun countSessionsByExercise(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<ExerciseCountRecord> {
        countCalls += fromEpochDay to toEpochDay
        return counts(fromEpochDay, toEpochDay)
    }

    override suspend fun purgeEmptyInProgress(): Int = unused()

    private fun unused(): Nothing = error("not used by these tests")
}

/** Conteneur de test : aucune dépendance Android ni stockage. */
class FakeAppContainer(
    override val settingsRepository: SettingsRepository = FakeSettingsRepository(),
    override val historyRepository: HistoryRepository = FakeHistoryRepository(),
    override val sessionArchive: SessionArchive = FakeSessionArchive(),
    override val endSignal: EndSignal = FakeEndSignal(),
    override val exerciseRegistry: ExerciseRegistry = ExerciseRegistry(emptyList()),
    override val clock: Clock = Clock.fixed(Instant.parse("2026-10-16T10:00:00Z"), ZoneOffset.UTC),
) : AppContainer {
    override val tableRepository: TableRepository = RealTables.repository
    override val exerciseCatalog: ExerciseCatalog = SettingsExerciseCatalog(settingsRepository)
    override val exerciseDependencies: ExerciseDependencies
        get() = error("not used by these tests")
}
