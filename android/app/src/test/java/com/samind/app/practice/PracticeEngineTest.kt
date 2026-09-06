package com.samind.app.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeEngineTest {

    private val box = BreathPattern.BOX_4444

    @Test
    fun phasesFollowThePatternInOrder() {
        assertEquals(BreathPhase.INHALE, box.at(0f).first)
        assertEquals(BreathPhase.HOLD, box.at(5f).first)
        assertEquals(BreathPhase.EXHALE, box.at(9f).first)
        assertEquals(BreathPhase.WAIT, box.at(13f).first)
    }

    @Test
    fun phaseFillIsContinuousNotStepped() {
        // design: the side fills smoothly in real time, not in per-second jumps
        val (_, quarter) = box.at(1f)
        val (_, half) = box.at(2f)
        assertEquals(0.25f, quarter, 0.001f)
        assertEquals(0.5f, half, 0.001f)
    }

    @Test
    fun patternRepeatsEveryCycle() {
        assertEquals(box.at(1f), box.at(1f + box.cycleSeconds))
        assertEquals(16, box.cycleSeconds)
    }

    @Test
    fun eightsPatternSkipsTheZeroLengthPhaseImmediately() {
        val eights = BreathPattern.EIGHTS
        assertEquals(BreathPhase.HOLD, eights.at(0f).first)
        assertEquals(56, eights.cycleSeconds)
    }

    @Test
    fun dotGridIsAlwaysFortyDotsAndScalesByDuration() {
        assertEquals(15f, secondsPerDot(10 * 60), 0.01f)   // 10 min -> 15 s per dot
        assertEquals(7.5f, secondsPerDot(5 * 60), 0.01f)
        assertEquals(30f, secondsPerDot(20 * 60), 0.01f)
        // a row of 8 dots takes 2 minutes at the 10-minute setting
        assertEquals(8, dotsFilled(120f, 10 * 60))
    }

    @Test
    fun dotsNeverExceedTheGrid() {
        assertEquals(PROGRESS_DOTS, dotsFilled(99_999f, 300))
        assertEquals(0, dotsFilled(0f, 300))
    }

    @Test
    fun eightsCountGrowsByEightPerCycle() {
        assertEquals(0, eightsCount(0f, 8))
        assertEquals(8, eightsCount(8f, 8))
        assertEquals(16, eightsCount(17f, 8))
    }

    @Test
    fun nextUnlocksOnlyWhenEveryCircleIsMarked() {
        assertFalse(stepComplete(setOf(0, 1, 2, 3), 0))     // step 1 needs 5
        assertTrue(stepComplete(setOf(0, 1, 2, 3, 4), 0))
        assertTrue(stepComplete(setOf(0), 4))               // step 5 needs 1
    }

    @Test
    fun completionTriggersOnTheFinalItemWithoutAnExtraTap() {
        assertFalse(sessionComplete(3, setOf(0, 1)))
        assertTrue(sessionComplete(4, setOf(0)))
    }

    @Test
    fun groundingStepsCountDownFromFive() {
        assertEquals(listOf(5, 4, 3, 2, 1), GROUNDING_STEP_ITEMS)
    }
}
