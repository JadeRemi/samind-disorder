package com.samind.app.ml

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

// Runs the shared vectors from ml/tests/data/golden_wordpiece.json (wired in as
// a test resource dir in build.gradle.kts). The Python mirror runs the same file;
// if either side drifts, exactly one of the suites goes red.
class GoldenWordPieceTest {

    private val golden = JSONObject(
        javaClass.classLoader!!.getResourceAsStream("golden_wordpiece.json")!!
            .bufferedReader().readText()
    )

    private fun vocabLines(): List<String> {
        if (golden.has("vocab")) {
            val arr = golden.getJSONArray("vocab")
            return (0 until arr.length()).map { arr.getString(it) }
        }
        return javaClass.classLoader!!
            .getResourceAsStream(golden.getString("vocab_file"))!!
            .bufferedReader().readLines()
    }

    @Test
    fun matchesSharedGoldenVectors() {
        val tokenizer = WordPieceTokenizer(vocabLines().asSequence(), golden.getInt("max_len"))

        val cases = golden.getJSONArray("cases")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            val text = case.getString("text")
            val idsJson = case.getJSONArray("ids")
            val expected = (0 until idsJson.length()).map { idsJson.getInt(it) }

            val enc = tokenizer.encode(text)
            assertEquals(text, expected, enc.inputIds.take(expected.size))
            assertEquals(text, expected.size, enc.attentionMask.count { it == 1 })
            assertEquals(text, golden.getInt("max_len"), enc.inputIds.size)
        }
    }
}
