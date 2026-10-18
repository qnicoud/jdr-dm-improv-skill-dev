package io.github.qnicoud.dmgym.core.exercise

import kotlinx.serialization.Serializable
import kotlin.time.Duration

/** Identifiant stable d'un exercice (clé de réglages, d'historique et de route). */
@JvmInline
@Serializable
value class ExerciseId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "exercise id must not be blank" }
    }

    companion object {
        val CRAZY_EIGHTS = ExerciseId("crazy_eights")
        val CONSTRAINT_BOX = ExerciseId("constraint_box")
        val CORE_IMAGE = ExerciseId("core_image")
        val STATUS_NUMBERS = ExerciseId("status_numbers")
        val DECISION_POINT = ExerciseId("decision_point")
        val CONNECT_STORY = ExerciseId("connect_story")
        val ONE_PAGE = ExerciseId("one_page")

        // v1.1 : un identifiant par exercice, une ligne chacun (V2b et V2c ajoutent la leur ici).
        val FAILING_FORWARD = ExerciseId("failing_forward")

        // Le catalogue est dérivé du registre. `by lazy` évite la dépendance circulaire à l'initialisation :
        // les définitions lisent les constantes ci-dessus, le registre les définitions.

        /** Tous les exercices, dans l'ordre d'affichage de l'accueil (celui du registre). */
        val ALL: List<ExerciseId> by lazy { ExerciseRegistry.Default.all.map { it.id } }

        /** Durées par défaut : celles de la politique de minuteur de chaque définition (One-Page : conseillée). */
        val DEFAULT_DURATIONS: Map<ExerciseId, Duration> by lazy {
            ExerciseRegistry.Default.all
                .mapNotNull { d -> d.timer.defaultDuration?.let { d.id to it } }
                .toMap()
        }

        /** Exercices courts éligibles comme échauffement de la séance du jour (ADR 0011) : `warmUp` des définitions. */
        val DAILY_WARM_UPS: Set<ExerciseId> by lazy {
            ExerciseRegistry.Default.all
                .filter { it.warmUp }
                .map { it.id }
                .toSet()
        }

        val UNTIMED_BY_DEFAULT: Set<ExerciseId> = setOf(ONE_PAGE)
    }
}
