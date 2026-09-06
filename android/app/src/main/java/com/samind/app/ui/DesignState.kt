package com.samind.app.ui

/**
 * Debug-only entry points so every frame in the design file can be opened
 * directly and photographed for the parity check. Passed as
 * `--es state <name>` alongside the destination; ignored in normal use.
 */
object DesignState {

    const val EXTRA = "state"

    // sign in
    const val FOCUSED = "focused"
    const val TYPED = "typed"

    // grounding
    const val STEP1 = "step1"
    const val STEP1_FILLED = "step1_filled"
    const val STEP2 = "step2"
    const val STEP3 = "step3"
    const val STEP4 = "step4"
    const val STEP5 = "step5"
    const val DONE = "done"

    // breathing
    const val INHALE = "inhale"
    const val HOLD = "hold"
    const val EXHALE = "exhale"
    const val WAIT = "wait"
    const val PAUSED = "paused"

    // count-eights / voice
    const val ACTIVE = "active"

    // chat
    const val MESSAGE = "message"
    const val REPLY = "reply"
    const val LOADING = "loading"
    const val SCROLLED = "scrolled"

    fun groundingStep(state: String?): Int? = when (state) {
        STEP1, STEP1_FILLED -> 0
        STEP2 -> 1
        STEP3 -> 2
        STEP4 -> 3
        STEP5, DONE -> 4
        else -> null
    }

    /** Marked items to preselect so a step renders in its "filled" variant. */
    fun groundingMarked(state: String?, itemsInStep: Int): Set<Int> = when (state) {
        STEP1_FILLED -> (0 until itemsInStep).toSet()
        DONE -> (0 until itemsInStep).toSet()
        else -> emptySet()
    }

    /** Seconds into the session that land the breathing figure in each phase. */
    fun breathingElapsed(state: String?, seconds: List<Int>): Float? {
        val cycle = seconds.sum().toFloat()
        return when (state) {
            INHALE -> seconds[0] * 0.5f
            HOLD -> seconds[0] + seconds[1] * 0.5f
            EXHALE -> seconds[0] + seconds[1] + seconds[2] * 0.5f
            WAIT, PAUSED -> cycle - seconds[3] * 0.5f
            DONE -> Float.MAX_VALUE
            else -> null
        }
    }
}
