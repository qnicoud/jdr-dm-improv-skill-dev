package io.github.qnicoud.dmgym.data.smoke

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
interface SmokeDao {
    @Insert
    suspend fun insert(entity: SmokeEntity): Long

    @Query("SELECT * FROM smoke WHERE id = :id")
    suspend fun findById(id: Long): SmokeEntity?
}
