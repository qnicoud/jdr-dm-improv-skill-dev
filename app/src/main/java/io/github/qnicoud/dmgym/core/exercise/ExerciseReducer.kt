package io.github.qnicoud.dmgym.core.exercise

/**
 * Réducteur pur de la session : même entrée, même sortie. Une action sans effet, ou illégale dans la phase
 * courante, renvoie [state] tel quel.
 */
fun reduce(
    state: ExerciseSession,
    action: ExerciseAction,
): ExerciseSession =
    when (action) {
        is ExerciseAction.Drawn -> {
            if (state.phase == Phase.Ready) state.copy(draws = action.draws) else state
        }

        is ExerciseAction.Rerolled -> {
            if (state.phase == Phase.Ended) state else reroll(state, action)
        }

        is ExerciseAction.Started -> {
            start(state, action)
        }

        is ExerciseAction.AnswerChanged -> {
            if (state.phase == Phase.Ended) state else changeAnswer(state, action)
        }

        is ExerciseAction.FlagToggled -> {
            toggleFlag(state, action.key)
        }

        is ExerciseAction.AutoDetected -> {
            autoDetect(state, action.detected)
        }

        is ExerciseAction.CheckToggled -> {
            toggleCheck(state, action.key)
        }

        is ExerciseAction.NextQuestion -> {
            state.copy(currentQuestion = (state.currentQuestion + 1).coerceIn(0, maxOf(0, action.lastIndex)))
        }

        is ExerciseAction.OptionChanged -> {
            state.copy(options = state.options + (action.key to action.value))
        }

        is ExerciseAction.EventFired -> {
            fireEvent(state, action)
        }

        ExerciseAction.Finished -> {
            if (state.phase == Phase.Active) state.copy(phase = Phase.Ended) else state
        }

        is ExerciseAction.Assessed -> {
            if (state.phase == Phase.Ended) {
                state.copy(selfRating = action.rating, isGem = action.isGem, rubric = action.rubric)
            } else {
                state
            }
        }
    }

private fun reroll(
    state: ExerciseSession,
    action: ExerciseAction.Rerolled,
): ExerciseSession {
    val draws = if (action.slotKey == null) action.draws else replaceSlots(state.draws, action.draws)
    return state.copy(draws = draws, rerollCount = state.rerollCount + 1)
}

private fun start(
    state: ExerciseSession,
    action: ExerciseAction.Started,
): ExerciseSession =
    if (state.phase == Phase.Ready) state.copy(phase = Phase.Active, sessionId = action.sessionId) else state

private fun changeAnswer(
    state: ExerciseSession,
    action: ExerciseAction.AnswerChanged,
): ExerciseSession {
    val current = state.answers[action.key]
    return when {
        current == null && action.text.isEmpty() -> state
        current?.text == action.text -> state
        else -> state.copy(answers = state.answers + (action.key to (current ?: Answer()).copy(text = action.text)))
    }
}

private fun toggleFlag(
    state: ExerciseSession,
    key: AnswerKey,
): ExerciseSession {
    val current = state.answers[key] ?: Answer()
    return state.copy(answers = state.answers + (key to current.copy(flagged = !current.flagged)))
}

private fun autoDetect(
    state: ExerciseSession,
    detected: Map<DrawKey, Boolean>,
): ExerciseSession {
    val draws =
        state.draws.map { draw ->
            val auto = detected[DrawKey(draw.slotKey, draw.position)] ?: return@map draw
            draw.copy(check = ElementCheck(auto = auto, manual = draw.check?.manual))
        }
    return if (draws == state.draws) state else state.copy(draws = draws)
}

private fun toggleCheck(
    state: ExerciseSession,
    key: DrawKey,
): ExerciseSession =
    state.copy(
        draws =
            state.draws.map { draw ->
                if (draw.slotKey != key.slotKey || draw.position != key.position) {
                    draw
                } else {
                    val check = draw.check ?: ElementCheck(auto = false)
                    draw.copy(check = check.copy(manual = !check.checked))
                }
            },
    )

/**
 * Remplace les tirages des slots présents dans [fresh], à la place du premier ancien tirage de chaque slot
 * (ou à la fin si le slot était absent), pour garder l'ordre d'affichage. Les nouvelles cases sont vierges.
 */
private fun replaceSlots(
    old: List<Draw>,
    fresh: List<Draw>,
): List<Draw> =
    fresh.groupBy { it.slotKey }.entries.fold(old) { acc, (slotKey, draws) ->
        val index = acc.indexOfFirst { it.slotKey == slotKey }
        val kept = acc.filter { it.slotKey != slotKey }
        if (index < 0) kept + draws else kept.take(index) + draws + kept.drop(index)
    }

/** Idempotent : un événement déjà déclenché, ou une séance terminée, ne change rien. */
private fun fireEvent(
    state: ExerciseSession,
    action: ExerciseAction.EventFired,
): ExerciseSession =
    if (action.eventId in state.firedEvents || state.phase == Phase.Ended) {
        state
    } else {
        state.copy(draws = replaceSlots(state.draws, action.draws), firedEvents = state.firedEvents + action.eventId)
    }
