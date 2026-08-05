package com.samind.app.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ln

data class Classification(val risky: Boolean, val score: Float)

// Three tiers, best available wins: transformer (model + vocab in assets),
// hashed-trigram dense net, lexicon rules. The app works with any of them.
class TriggerClassifier(context: Context) {

    private val transformer: Interpreter? = loadInterpreter(context, TRANSFORMER_FILE)
    private val tokenizer: WordPieceTokenizer? = if (transformer != null) {
        try {
            context.assets.open(VOCAB_FILE).bufferedReader().useLines { lines ->
                WordPieceTokenizer(lines, SEQ_LEN)
            }
        } catch (e: Exception) {
            null
        }
    } else null

    private val trigramModel: Interpreter? =
        if (transformer == null) loadInterpreter(context, TRIGRAM_FILE) else null

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

        transformerScore(text)?.let { score ->
            return Classification(score >= MODEL_THRESHOLD, score)
        }
        trigramScore(text)?.let { score ->
            return Classification(score >= MODEL_THRESHOLD, score)
        }

        val hits = lexicon.count { it.containsMatchIn(text) }
        val score = (hits / 2f).coerceAtMost(1f)
        return Classification(hits > 0, score)
    }

    private fun transformerScore(text: String): Float? {
        val model = transformer ?: return null
        val enc = (tokenizer ?: return null).encode(text.take(MAX_CHARS))
        val inputs = arrayOf<Any>(arrayOf(enc.inputIds), arrayOf(enc.attentionMask))
        val probabilities = Array(1) { FloatArray(2) }
        return try {
            model.runForMultipleInputsOutputs(inputs, mapOf(0 to probabilities))
            probabilities[0][1]
        } catch (e: Exception) {
            null
        }
    }

    private fun trigramScore(text: String): Float? {
        val model = trigramModel ?: return null
        val output = arrayOf(floatArrayOf(0f))
        return try {
            model.run(arrayOf(features(text)), output)
            output[0][0]
        } catch (e: Exception) {
            null
        }
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

    private fun loadInterpreter(context: Context, assetName: String): Interpreter? = try {
        context.assets.openFd(assetName).createInputStream().use { stream ->
            val bytes = stream.readBytes()
            val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            buffer.put(bytes)
            Interpreter(buffer)
        }
    } catch (e: Exception) {
        null
    }

    fun close() {
        transformer?.close()
        trigramModel?.close()
    }

    companion object {
        private const val TRANSFORMER_FILE = "trigger_transformer.tflite"
        private const val VOCAB_FILE = "vocab.txt"
        private const val TRIGRAM_FILE = "trigger_classifier.tflite"
        private const val FEATURE_DIM = 2048
        private const val SEQ_LEN = 128
        private const val MODEL_THRESHOLD = 0.75f
        private const val MIN_LENGTH = 12
        private const val MAX_CHARS = 500
    }
}
