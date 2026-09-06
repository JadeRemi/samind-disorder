package com.samind.app.practice

/**
 * Pure logic for the timed practices — no Android types, so it is unit-tested.
 * Rules come from the design annotations (see docs/DEV_PLAN_REDESIGN.md).
 */

enum class BreathPhase { INHALE, HOLD, EXHALE, WAIT }

/** Box-breathing technique: seconds per phase, in order. */
data class BreathPattern(val id: String, val seconds: List<Int>) {
    init {
        require(seconds.size == 4) { "a box pattern has four phases" }
    }

    val cycleSeconds: Int get() = seconds.sum()

    /** Phase and its progress (0..1) at [elapsed] seconds into the session. */
    fun at(elapsed: Float): Pair<BreathPhase, Float> {
        val inCycle = elapsed.mod(cycleSeconds.toFloat())
        var start = 0f
        for ((index, length) in seconds.withIndex()) {
            val end = start + length
            if (inCycle < end || index == seconds.lastIndex) {
                val fraction = if (length == 0) 1f else ((inCycle - start) / length)
                return BreathPhase.entries[index] to fraction.coerceIn(0f, 1f)
            }
            start = end
        }
        error("unreachable")
    }

    companion object {
        val BOX_4444 = BreathPattern("4-4-4-4", listOf(4, 4, 4, 4))
        val EIGHTS = BreathPattern("0-8-16-32", listOf(0, 8, 16, 32))
        val all = listOf(BOX_4444, EIGHTS)
        fun byId(id: String) = all.find { it.id == id } ?: BOX_4444
    }
}

/** Session durations offered in practice settings. */
val DURATION_OPTIONS_MINUTES = listOf(5, 10, 20)

/**
 * The dot grid always has 40 dots regardless of session length; only the fill
 * rate changes (design annotation).
 */
const val PROGRESS_DOTS = 40

fun secondsPerDot(sessionSeconds: Int): Float = sessionSeconds.toFloat() / PROGRESS_DOTS

fun dotsFilled(elapsedSeconds: Float, sessionSeconds: Int): Int =
    (elapsedSeconds / secondsPerDot(sessionSeconds)).toInt().coerceIn(0, PROGRESS_DOTS)

/** Count-eights: the number shown grows by 8 each breath cycle. */
fun eightsCount(elapsedSeconds: Float, cycleSeconds: Int): Int =
    (elapsedSeconds / cycleSeconds).toInt() * 8

/** 5-4-3-2-1: how many items each step asks for. */
val GROUNDING_STEP_ITEMS = listOf(5, 4, 3, 2, 1)

/** "Next" unlocks only when every circle of the step is marked. */
fun stepComplete(marked: Set<Int>, stepIndex: Int): Boolean =
    marked.size >= GROUNDING_STEP_ITEMS[stepIndex]

/** The completion modal opens by itself once the final item is marked. */
fun sessionComplete(stepIndex: Int, marked: Set<Int>): Boolean =
    stepIndex == GROUNDING_STEP_ITEMS.lastIndex && stepComplete(marked, stepIndex)
