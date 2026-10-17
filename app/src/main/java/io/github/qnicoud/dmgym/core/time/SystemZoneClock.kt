package io.github.qnicoud.dmgym.core.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Horloge système dont le fuseau est relu à chaque appel : un changement de fuseau pendant que le processus vit
 * est pris en compte (`Clock.systemDefaultZone()` fige le fuseau à sa création). Le jour local suit donc le téléphone.
 */
class SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
