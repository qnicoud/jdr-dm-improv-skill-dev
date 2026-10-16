package io.github.qnicoud.dmgym.core.time

import java.time.Clock
import java.time.LocalDate

/** Jour local (fuseau de l'horloge) exprimé en jours depuis l'epoch ; bascule à minuit local. */
fun Clock.localEpochDay(): Long = LocalDate.now(this).toEpochDay()
