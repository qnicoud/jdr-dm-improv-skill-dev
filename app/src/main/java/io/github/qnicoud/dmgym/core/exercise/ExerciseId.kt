package io.github.qnicoud.dmgym.core.exercise

/** Les 7 exercices de la v1, dans l'ordre d'affichage de l'accueil. [key] est stable : il sert aux routes et à la base. */
enum class ExerciseId(
    val key: String,
) {
    CRAZY_EIGHTS("crazy_eights"),
    CONSTRAINT_BOX("constraint_box"),
    CORE_IMAGE("core_image"),
    STATUS_NUMBERS("status_numbers"),
    DECISION_POINT("decision_point"),
    CONNECT_STORY("connect_story"),
    ONE_PAGE("one_page"),
    ;

    companion object {
        fun fromKey(key: String): ExerciseId? = entries.firstOrNull { it.key == key }
    }
}
