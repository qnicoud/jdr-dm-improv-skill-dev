package io.github.qnicoud.dmgym.platform

import android.os.SystemClock
import io.github.qnicoud.dmgym.core.time.MonotonicClock

/** `SystemClock.elapsedRealtime()` : monotone, compte aussi la veille (ADR 0006). */
class SystemMonotonicClock : MonotonicClock {
    override fun now(): Long = SystemClock.elapsedRealtime()
}
