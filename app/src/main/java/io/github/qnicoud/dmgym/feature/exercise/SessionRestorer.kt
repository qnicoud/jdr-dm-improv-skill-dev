package io.github.qnicoud.dmgym.feature.exercise

import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.lastAnsweredQuestion
import io.github.qnicoud.dmgym.core.exercise.timerSnapshotOf
import io.github.qnicoud.dmgym.core.exercise.toExerciseSession
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Clés du `SavedStateHandle` partagées entre le ViewModel et la reprise. */
internal object HandleKeys {
    const val SESSION = "session_id"
    const val TIMER = "timer"
    const val QUESTION = "question"
    const val LANGUAGE = "content_language"
}

/** Départ de l'écran : modèle neuf, ou session à reprendre si [detail] est présent. */
internal class StartState(
    val model: ExerciseModel,
    val detail: SessionDetail?,
)

/** Construit l'état initial d'un exercice, neuf ou repris depuis l'historique et le `SavedStateHandle`. */
internal class SessionRestorer(
    private val definition: ExerciseDefinition,
    private val deps: ExerciseDependencies,
    private val handle: SavedStateHandle,
    private val launch: ExerciseLaunch,
    private val engine: TimerEngine,
) {
    /** Lit les réglages et, si une session est à reprendre pour cet exercice, son détail. */
    suspend fun start(): StartState {
        val settings = deps.settings.settings.first()
        val (durationMs, untimed) = timerConfig(definition, settings)
        val defaults = definition.options.associate { it.key to it.default }
        val model =
            ExerciseModel(
                session = ExerciseSession(definition.id, options = defaults, language = sessionLanguage()),
                timer = TimerState.Idle(durationMs),
                untimed = untimed,
            )
        val resumeId = handle.get<Long>(HandleKeys.SESSION) ?: launch.resumeSessionId
        val detail = resumeId?.let { withContext(deps.ioDispatcher) { deps.history.observeDetail(it).first() } }
        return StartState(model, detail?.takeIf { it.session.exerciseId == definition.id.value })
    }

    /** Langue d'une séance neuve : celle déjà gardée dans le `SavedStateHandle`, sinon la langue courante de l'app. */
    private fun sessionLanguage(): ContentLanguage =
        ContentLanguage.fromCode(handle.get<String>(HandleKeys.LANGUAGE))
            ?: deps.language.current.value
                .also { handle[HandleKeys.LANGUAGE] = it.code }

    /** Modèle d'une session reprise : minuteur restauré (ou en pause), question et groupes retrouvés. */
    fun restored(detail: SessionDetail): ExerciseModel {
        val session = detail.toExerciseSession()
        val snapshot = timerSnapshotOf(handle.get<LongArray>(HandleKeys.TIMER))
        val now = deps.monotonicClock.now()
        val timer = snapshot?.let { engine.restore(it, deps.wallClock.millis(), now) } ?: pausedTimerFor(detail.session)
        return ExerciseModel(
            session.copy(currentQuestion = handle.get<Int>(HandleKeys.QUESTION) ?: lastAnsweredQuestionOf(session)),
            timer,
            untimed = timer is TimerState.Untimed || timer is TimerState.UntimedPaused,
            loading = false,
            extraGroups = session.answers.keys.maxOfOrNull { it.groupIndex } ?: 0,
        )
    }

    /** Sans état gardé, une session à questions successives reprend à la dernière question répondue. */
    private fun lastAnsweredQuestionOf(session: ExerciseSession): Int {
        val layout = definition.layout as? FieldLayout.Sequential ?: return 0
        val answered =
            session.answers
                .filter { it.value.text.isNotBlank() }
                .keys
                .map { it.groupIndex }
        return lastAnsweredQuestion(answered, session.drawsOf(layout.questionsSlot).size)
    }
}
