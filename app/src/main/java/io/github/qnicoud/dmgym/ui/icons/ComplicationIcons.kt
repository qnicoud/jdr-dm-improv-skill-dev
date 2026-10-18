package io.github.qnicoud.dmgym.ui.icons

/** Icônes des six types de complication d'Échec qui avance (24 dp, trait 2), même gabarit que [SenseIcons]. */
object ComplicationIcons {
    val Cost =
        icon(
            "complication_cost",
            circle(12f, 12f, 9f),
            "M14.5 9.5c-.5-1-1.5-1.5-2.5-1.5-1.5 0-2.5.8-2.5 2s1 1.7 2.5 2 2.5.8 2.5 2-1 2-2.5 2c-1 0-2-.5-2.5-1.5M12 6v2M12 16v2",
        )
    val Time = icon("complication_time", circle(12f, 12f, 9f), "M12 7v5l3 2")
    val Information = icon("complication_information", circle(12f, 12f, 9f), "M12 11v6M12 7.5v.5")
    val Relation =
        icon(
            "complication_relation",
            circle(9f, 8f, 3f),
            "M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6",
            circle(17f, 9f, 2.5f),
            "M17 14c2.5 0 4 1.8 4 4.5",
        )
    val Position = icon("complication_position", "M12 4v13M6 11l6 6 6-6M5 21h14")
    val Danger = icon("complication_danger", "M13 2L4 14h7l-1 8 9-12h-7l1-8z")
}
