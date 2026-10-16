package io.github.qnicoud.dmgym.data.history.db

import androidx.room3.Embedded
import androidx.room3.Relation

data class SessionWithChildren(
    @Embedded val session: SessionEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["sessionId"]) val draws: List<DrawEntity>,
    @Relation(parentColumns = ["id"], entityColumns = ["sessionId"]) val answers: List<AnswerEntity>,
    @Relation(parentColumns = ["id"], entityColumns = ["sessionId"]) val rubric: List<RubricEntity>,
)

data class ExerciseCount(
    val exerciseId: String,
    val n: Int,
)
