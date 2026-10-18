package io.github.qnicoud.dmgym.core.tables

import io.github.qnicoud.dmgym.core.i18n.ContentLanguage

/**
 * Tables shipped with the app (ADR 0009). [fileName] is the asset name without extension; the file lives in
 * `<base>/<language>/` (ADR 0016 §2), see [assetPath].
 */
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
    ;

    /** Path of this table for [language], relative to the assets root: `tables/fr/problems.json`. */
    fun assetPath(
        language: ContentLanguage,
        basePath: String = TABLES_DIR,
    ): String = "$basePath/${language.code}/$fileName.json"

    companion object {
        const val TABLES_DIR = "tables"
    }
}
