package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.AnswerDetector
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession

/** Détection des mots tirés dans le texte saisi (ADR 0008) : cases automatiques, mots-clés et surlignage. */
internal class DetectionCoordinator(
    private val definition: ExerciseDefinition,
    private val baseDetector: AnswerDetector?,
    private val dispatch: (ExerciseAction) -> Unit,
    private val session: () -> ExerciseSession,
) {
    /** Détecteur de la langue de la séance en cours. */
    private val detector: AnswerDetector? get() = baseDetector?.forLanguage(session().language)

    /** Recalcule les cases « utilisé » ; le cochage manuel n'est jamais touché (réducteur). */
    fun redetect() {
        val spec = definition.detection ?: return
        val detector = detector ?: return
        val current = session()
        val draws = current.draws.filter { it.slotKey in spec.slotKeys }
        val text = current.textOf(AnswerKey(spec.field))
        dispatch(ExerciseAction.AutoDetected(detector.detect(draws, text), detector.occurrences(draws, text)))
    }

    /** Charge les mots-clés de [draws] ; à faire avant que l'écran n'affiche le champ, pour un surlignage immédiat. */
    suspend fun loadKeywords(draws: List<Draw>) {
        val spec = definition.detection ?: return
        detector?.prepare(draws.filter { it.slotKey in spec.slotKeys })
    }

    fun redetectIfTyped() {
        val spec = definition.detection ?: return
        if (session().textOf(AnswerKey(spec.field)).isNotBlank()) redetect()
    }

    /** Après un nouveau tirage (relance, option) : charge les mots-clés des tirages puis recalcule les cases. */
    suspend fun prepare() {
        loadKeywords(session().draws)
        redetectIfTyped()
    }

    /** Plages du [text] à surligner (champ surveillé) ; synchrone, appelé à chaque rendu du champ. */
    fun highlights(text: String): List<IntRange> {
        val spec = definition.detection ?: return emptyList()
        val detector = detector ?: return emptyList()
        return detector.highlights(session().draws.filter { it.slotKey in spec.slotKeys }, text)
    }
}
