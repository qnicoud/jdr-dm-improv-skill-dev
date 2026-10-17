package io.github.qnicoud.dmgym.core.streak

/** Nombre de jours de la rangée glissante affichée dans Progrès. */
const val RECENT_DAYS = 7

/**
 * Résumé de la pratique (ADR 0011, D15, D35).
 *
 * @property totalSessions sessions comptées, affichées en premier (D32).
 * @property current jours pratiqués de la série courante ; le jour de grâce n'y figure pas.
 * @property graceUsedOn jour de grâce le plus récent de la série courante.
 * @property graceDays tous les jours de grâce de la série courante.
 */
data class StreakSummary(
    val totalSessions: Int,
    val practicedDays: Int,
    val current: Int,
    val best: Int,
    val graceUsedOn: Long?,
    val todayDone: Boolean,
    val graceDays: Set<Long> = emptySet(),
) {
    /** Vrai si le jour de grâce a servi dans la semaine (du lundi au dimanche) de [today] : « rechargé lundi » (D46). */
    fun graceUsedThisWeek(today: Long): Boolean = graceUsedOn?.let { weekStart(it) == weekStart(today) } ?: false
}

enum class DayMark { PRACTICED, GRACE, NONE }

data class DayStatus(
    val epochDay: Long,
    val mark: DayMark,
)

/** Lundi de la semaine contenant [epochDay] (le jour 0 de l'époque est un jeudi). */
fun weekStart(epochDay: Long): Long = epochDay - Math.floorMod(epochDay + 3, 7L)

private data class Run(
    val length: Int,
    val lastDay: Long,
    val graces: List<Long>,
) {
    fun canUseGrace(missing: Long): Boolean = graces.none { weekStart(it) == weekStart(missing) }
}

/**
 * Série de jours, calcul pur sur des `localEpochDay` (aucune horloge n'est lue).
 *
 * - Aujourd'hui non pratiqué ne rompt rien : on part d'hier.
 * - Un jour manquant isolé entre deux jours pratiqués est couvert par le jour de grâce, au plus un par semaine
 *   (lundi à dimanche) : la grâce est rechargée chaque lundi. Il ne compte pas dans la série.
 * - Deux jours manquants d'affilée, ou une seconde grâce dans la même semaine, terminent la série.
 * - `best`, `practicedDays` et `totalSessions` ne baissent jamais.
 * - Les jours postérieurs à [today] (horloge réglée dans le passé) sont ignorés.
 */
fun computeStreak(
    days: Set<Long>,
    sessions: Int,
    today: Long,
): StreakSummary {
    val practiced = days.filter { it <= today }.sorted()
    val runs = buildRuns(practiced)
    val (current, graces) = currentRun(runs.lastOrNull(), today)
    return StreakSummary(
        totalSessions = sessions,
        practicedDays = practiced.size,
        current = current,
        best = maxOf(runs.maxOfOrNull { it.length } ?: 0, current),
        graceUsedOn = graces.maxOrNull(),
        todayDone = today in days,
        graceDays = graces.toSet(),
    )
}

private fun buildRuns(practiced: List<Long>): List<Run> {
    val finished = mutableListOf<Run>()
    var run: Run? = null
    for (day in practiced) {
        val open = run
        run =
            when {
                open == null -> {
                    Run(1, day, emptyList())
                }

                day - open.lastDay == 1L -> {
                    open.copy(length = open.length + 1, lastDay = day)
                }

                day - open.lastDay == 2L && open.canUseGrace(open.lastDay + 1) -> {
                    Run(open.length + 1, day, open.graces + (open.lastDay + 1))
                }

                else -> {
                    finished += open
                    Run(1, day, emptyList())
                }
            }
    }
    return finished + listOfNotNull(run)
}

/** Longueur et jours de grâce de la série encore ouverte à [today] ; (0, vide) si elle est terminée. */
private fun currentRun(
    run: Run?,
    today: Long,
): Pair<Int, List<Long>> {
    if (run == null) return 0 to emptyList()
    val missing = today - 1
    return when {
        run.lastDay >= today - 1 -> run.length to run.graces
        run.lastDay == today - 2 && run.canUseGrace(missing) -> run.length to (run.graces + missing)
        else -> 0 to emptyList()
    }
}

/** Les [RECENT_DAYS] derniers jours, du plus ancien à aujourd'hui (à droite). */
fun recentDays(
    summary: StreakSummary,
    days: Set<Long>,
    today: Long,
): List<DayStatus> =
    ((today - RECENT_DAYS + 1)..today).map { day ->
        val mark =
            when {
                day in days -> DayMark.PRACTICED
                day in summary.graceDays -> DayMark.GRACE
                else -> DayMark.NONE
            }
        DayStatus(day, mark)
    }
