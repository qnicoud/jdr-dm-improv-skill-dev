package io.github.qnicoud.dmgym.ui.icons

/** Icônes de trait de l'écran d'exercice (même gabarit que [DmIcons]). */
object ExerciseIcons {
    val Pause = icon("pause", "M8 5v14M16 5v14")
    val Play = icon("play", "M7 4l13 8-13 8z")
    val Hourglass = icon("hourglass", "M6 3h12M6 21h12M7 3v4l5 5-5 5v4M17 3v4l-5 5 5 5v4")
    val AddCircle = icon("add_circle", circle(12f, 12f, 9f), "M12 8v8M8 12h8")
    val Dice =
        icon(
            "dice",
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
            circle(8f, 8f, 1f),
            circle(16f, 16f, 1f),
            circle(12f, 12f, 1f),
        )
    val Diamond = icon("diamond", "M6 3h12l4 6-10 12L2 9zM2 9h20")
    val Check = icon("check", "M5 12l5 5 9-10")
    val Edit = icon("edit", "M4 20h4L19 9l-4-4L4 16z")
    val Build = icon("build", "M4 20h16M6 20V10l6-6 6 6v10")
    val Shield = icon("shield", "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z")
}
