package io.github.qnicoud.dmgym.ui.icons

/** Icônes de l'historique et du détail de session, reprises des maquettes (09-historique). */
object HistoryIcons {
    val Share =
        icon(
            "share",
            circle(18f, 5f, 3f),
            circle(6f, 12f, 3f),
            circle(18f, 19f, 3f),
            "M8.6 10.5l6.8-4M8.6 13.5l6.8 4",
        )
    val More = icon("more", circle(12f, 5f, 1f), circle(12f, 12f, 1f), circle(12f, 19f, 1f))
    val Trash = icon("trash", "M5 7h14M9 7V4h6v3M7 7l1 13h8l1-13")
    val Circle = icon("circle", circle(12f, 12f, 8f))
}
