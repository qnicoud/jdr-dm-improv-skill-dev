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

private val ALL_CARDS = 1..10
private val HIGH_RANK_CARDS = 7..10
private val MID_RANK_CARDS = 4..7
private val LOW_RANK_CARDS = 1..4

/**
 * Cartes à tirer selon l'étiquette `rank:*` du rôle (recette rc1, critère 1.8, D45). Par défaut le statut suit le
 * rang : haut → 7-10, moyen → 4-7, bas → 1-4. Avec [counter] (contre-stéréotype) il prend la plage opposée :
 * haut → 1-3, bas → 8-10, moyen → 1-3 ou 8-10. Sans étiquette de rang, toutes les cartes (1-10).
 */
fun statusCards(
    tags: List<String>,
    counter: Boolean,
): List<Int> =
    when {
        "rank:high" in tags -> if (counter) LOW_CARDS.toList() else HIGH_RANK_CARDS.toList()
        "rank:low" in tags -> if (counter) HIGH_CARDS.toList() else LOW_RANK_CARDS.toList()
        "rank:mid" in tags -> if (counter) (LOW_CARDS + HIGH_CARDS).toList() else MID_RANK_CARDS.toList()
        else -> ALL_CARDS.toList()
    }
