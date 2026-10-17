package io.github.qnicoud.dmgym.app

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Horloge de test dont l'instant et le fuseau peuvent changer (minuit, voyage, horloge reculée). */
class MutableClock(
    var now: Instant,
    private var zone: ZoneId = ZoneId.of("UTC"),
) : Clock() {
    fun setZone(newZone: ZoneId) {
        zone = newZone
    }

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

    override fun instant(): Instant = now
}
