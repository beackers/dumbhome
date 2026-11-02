package com.beackers.dumbhome

class EmergencySequence(
    private val requiredPresses: Int = DEFAULT_REQUIRED_PRESSES,
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    private val clock: () -> Long = { android.os.SystemClock.elapsedRealtime() }
) {
    private var pressCount = 0
    private var lastPressTime = 0L

    fun press(): Boolean {
        val now = clock()
        pressCount = if (now - lastPressTime <= windowMillis) {
            pressCount + 1
        } else {
            1
        }
        lastPressTime = now

        if (pressCount < requiredPresses) return false

        reset()
        return true
    }

    fun reset() {
        pressCount = 0
        lastPressTime = 0L
    }

    companion object {
        const val DEFAULT_REQUIRED_PRESSES = 5
        const val DEFAULT_WINDOW_MILLIS = 5_000L
    }
}
