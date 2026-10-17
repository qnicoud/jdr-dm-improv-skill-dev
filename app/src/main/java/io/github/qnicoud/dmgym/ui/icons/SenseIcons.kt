package io.github.qnicoud.dmgym.ui.icons

/** Icônes des cinq sens (24 dp, trait 2), même gabarit que [DmIcons]. La forme porte le sens, jamais la couleur. */
object SenseIcons {
    val Sight =
        icon(
            "sense_sight",
            "M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7z",
            circle(12f, 12f, 3f),
        )
    val Hearing = icon("sense_hearing", "M7 10a5 5 0 0 1 10 0c0 3-3 3.5-3 6.5a2.5 2.5 0 0 1-5 0M10 10a2 2 0 0 1 4 0")
    val Smell = icon("sense_smell", "M3 9h11a3 3 0 1 0-3-3M3 15h14a3 3 0 1 1-3 3M3 12h8")
    val Touch =
        icon(
            "sense_touch",
            "M8 13V6.5a1.5 1.5 0 0 1 3 0V11M11 11V4.5a1.5 1.5 0 0 1 3 0V11M14 11V6.5a1.5 1.5 0 0 1 3 0V15c0 4-2 6-5.5 6-3 0-4.5-1.5-6-4.5L4 13.5a1.5 1.5 0 0 1 2.6-1.3L8 14",
        )
    val Taste = icon("sense_taste", "M3 12c3-4 6-4 9-2 3-2 6-2 9 2-3 5-6 6-9 6s-6-1-9-6zM3 12h18")
}
