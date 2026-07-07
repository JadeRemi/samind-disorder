package com.samind.app.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ln

data class Classification(val risky: Boolean, val score: Float)

class TriggerClassifier(context: Context) {

    private val interpreter: Interpreter? = try {
        val fd = context.assets.openFd(MODEL_FILE)
        fd.createInputStream().use { stream ->
            val bytes = stream.readBytes()
            val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            buffer.put(bytes)
            Interpreter(buffer)
        }
    } catch (e: Exception) {
        null
    }

    // Fallback lexicon, applied to normalized text when no model is bundled.
    private val lexicon = listOf(
        Regex("\\bthinspo\\b"),
        Regex("\\bmeanspo\\b"),
        Regex("\\bpro ?ana\\b"),
        Regex("\\bpro ?mia\\b"),
        Regex("\\bugw\\b"),
        Regex("\\bcw\\b.{0,12}\\bgw\\b"),
        Regex("\\bstarv\\w*"),
        Regex("\\bpurg\\w*"),
        Regex("skip\\w* (meals?|dinner|breakfast|lunch)"),
        Regex("(water|liquid) ?fast"),
        Regex("body ?check"),
        Regex("(collarbones?|ribs) .*(goals?|progress|visible)"),
        Regex("(burn|earn) .*(everything|every)? ?you (ate|eat)"),
        Regex("nothing tastes as good as"),
        Regex("low restriction"),
        Regex("(only|just) \\d{2,3} (kcal|cal(orie)?s?)( today| a day)?"),
    )

    fun classify(rawText: String): Classification {
        val text = TextNormalizer.normalize(rawText)
        if (text.length < MIN_LENGTH) return Classification(false, 0f)

        interpreter?.let {
            val input = arrayOf(features(text))
            val output = arrayOf(floatArrayOf(0f))
            it.run(input, output)
            val score = output[0][0]
            return Classification(score >= MODEL_THRESHOLD, score)
        }

        val hits = lexicon.count { it.containsMatchIn(text) }
        val score = (hits / 2f).coerceAtMost(1f)
        return Classification(hits > 0, score)
    }

    // Must match samind_ml/features.py: hashed char trigrams, log1p counts.
    private fun features(text: String, dim: Int = FEATURE_DIM): FloatArray {
        val v = FloatArray(dim)
        val s = "^$text$"
        for (i in 0..s.length - 3) {
            val h = s.substring(i, i + 3).hashCode()
            v[((h % dim) + dim) % dim] += 1f
        }
        for (i in v.indices) v[i] = ln(1f + v[i])
        return v
    }

    fun close() {
        interpreter?.close()
    }

    companion object {
        private const val MODEL_FILE = "trigger_classifier.tflite"
        private const val FEATURE_DIM = 2048
        private const val MODEL_THRESHOLD = 0.75f
        private const val MIN_LENGTH = 12
    }
}
