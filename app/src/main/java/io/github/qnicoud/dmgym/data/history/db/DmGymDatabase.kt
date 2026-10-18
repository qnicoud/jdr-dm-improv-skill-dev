package io.github.qnicoud.dmgym.data.history.db

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [SessionEntity::class, DrawEntity::class, AnswerEntity::class, RubricEntity::class],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
    exportSchema = true,
)
abstract class DmGymDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
