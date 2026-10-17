package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.AnswerDetector
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.signal.EndSignal
import io.github.qnicoud.dmgym.core.tables.TableRepository
import io.github.qnicoud.dmgym.core.time.MonotonicClock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import kotlin.random.Random

/** Collaborateurs de [ExerciseViewModel], regroupés pour garder les fabriques lisibles (ADR 0005). */
class ExerciseDependencies(
    val tables: TableRepository,
    val history: HistoryRepository,
    val settings: SettingsRepository,
    val endSignal: EndSignal,
    val monotonicClock: MonotonicClock,
    val wallClock: Clock,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val detector: AnswerDetector? = null,
)

/** Paramètres de lancement d'un exercice (route `Exercise`). */
data class ExerciseLaunch(
    val resumeSessionId: Long? = null,
    val fromDaily: Boolean = false,
    /** Graine du tirage ; fixée par les tests, aléatoire en production. */
    val seed: Long = Random.nextLong(),
)
