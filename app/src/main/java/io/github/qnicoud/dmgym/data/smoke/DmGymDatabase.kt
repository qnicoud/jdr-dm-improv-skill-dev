package io.github.qnicoud.dmgym.data.smoke

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(entities = [SmokeEntity::class], version = 1, exportSchema = true)
abstract class DmGymDatabase : RoomDatabase() {
    abstract fun smokeDao(): SmokeDao
}
