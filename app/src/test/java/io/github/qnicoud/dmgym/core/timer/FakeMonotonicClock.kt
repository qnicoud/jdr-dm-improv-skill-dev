package io.github.qnicoud.dmgym.core.timer

import io.github.qnicoud.dmgym.core.time.MonotonicClock

class FakeMonotonicClock(
    var nowMillis: Long = 0L,
) : MonotonicClock {
    override fun now(): Long = nowMillis

    fun advance(millis: Long) {
        nowMillis += millis
    }
}
