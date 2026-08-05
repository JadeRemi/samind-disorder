package com.samind.app.ml

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Budget from the task card: p95 < 50 ms for the baseline path.
// Run on a real mid-range device, not only an emulator.
@RunWith(AndroidJUnit4::class)
class ClassifierLatencyTest {

    private val samples = listOf(
        "skip dinner wake up thinner they said",
        "trying a new pasta recipe tonight with friends",
        "st4rv1ng is a lifestyle not a phase honestly",
        "finished a 5k run this morning feeling great",
        "cw 52 gw 44 body check after a three day fast",
        "meal prep sunday chicken rice and lots of veggies",
    )

    @Test
    fun p95LatencyWithinBudget() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val classifier = TriggerClassifier(context)
        try {
            repeat(WARMUP) { classifier.classify(samples[it % samples.size]) }
            val timesNs = LongArray(RUNS) { i ->
                val start = SystemClock.elapsedRealtimeNanos()
                classifier.classify(samples[i % samples.size])
                SystemClock.elapsedRealtimeNanos() - start
            }.sorted()
            val p95Ms = timesNs[(RUNS * 95 / 100) - 1] / 1_000_000.0
            val medianMs = timesNs[RUNS / 2] / 1_000_000.0
            assertTrue(
                "p95=%.1fms median=%.1fms (budget %dms)".format(p95Ms, medianMs, BUDGET_MS),
                p95Ms < BUDGET_MS,
            )
        } finally {
            classifier.close()
        }
    }

    companion object {
        private const val WARMUP = 10
        private const val RUNS = 100
        private const val BUDGET_MS = 50
    }
}
