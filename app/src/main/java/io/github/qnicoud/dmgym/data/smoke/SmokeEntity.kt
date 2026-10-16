package io.github.qnicoud.dmgym.data.smoke

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Entité de fumée du lot 1, remplacée par le schéma du §4 au lot 6. */
@Entity(tableName = "smoke")
data class SmokeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
)
