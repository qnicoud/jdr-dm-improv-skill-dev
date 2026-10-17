package io.github.qnicoud.dmgym.core.exercise

/** Niveau de statut d'une carte de 1 à 10, tel qu'écrit sur la carte (« Statut 8 sur 10 · haut »). */
enum class StatusLevel { Low, Medium, High }

private val LOW_CARDS = 1..3
private val HIGH_CARDS = 8..10

fun statusLevel(value: Int): StatusLevel =
    when (value) {
        in LOW_CARDS -> StatusLevel.Low
        in HIGH_CARDS -> StatusLevel.High
        else -> StatusLevel.Medium
    }

/**
 * Cartes à tirer pour que le statut soit à l'opposé du rang du rôle (D45) : rang haut → statut bas, et
 * inversement. `null` si le rôle n'a pas d'étiquette `rank:*`.
 */
fun contrastBand(tags: List<String>): IntRange? =
    when {
        "rank:high" in tags -> LOW_CARDS
        "rank:low" in tags -> HIGH_CARDS
        else -> null
    }
