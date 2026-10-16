package io.github.qnicoud.dmgym.core.tables

/** Tables shipped with the app (ADR 0009). [fileName] is the asset name without extension. */
enum class TableId(
    val fileName: String,
) {
    Problems("problems"),
    Places("places"),
    SensoryWords("sensory_words"),
    CoreImages("core_images"),
    ThreadingQuestions("threading_questions"),
    NpcRoles("npc_roles"),
    StatusShifts("status_shifts"),
    SurpriseSituations("surprise_situations"),
    StoryElements("story_elements"),
    OnePageSituations("one_page_situations"),
}
