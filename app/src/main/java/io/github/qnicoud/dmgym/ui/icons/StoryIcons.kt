package io.github.qnicoud.dmgym.ui.icons

/** Icônes des cinq types d'élément de Connect the Story (24 dp, trait 2), même gabarit que [SenseIcons]. */
object StoryIcons {
    val Place = icon("story_place", "M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z", circle(12f, 9.5f, 2.5f))
    val Object = icon("story_object", "M12 3l8 4.5v9L12 21l-8-4.5v-9L12 3zM4 7.5l8 4.5 8-4.5M12 12v9")
    val Trait = icon("story_trait", circle(12f, 8f, 4f), "M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7")
    val Sensory =
        icon(
            "story_sensory",
            "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3zM19 16l.7 1.8 1.8.7-1.8.7L19 21l-.7-1.8-1.8-.7 1.8-.7L19 16z",
        )
    val Line = icon("story_line", "M4 5h16v11H9l-5 4V5zM8 9.5h8M8 12.5h5")
}
