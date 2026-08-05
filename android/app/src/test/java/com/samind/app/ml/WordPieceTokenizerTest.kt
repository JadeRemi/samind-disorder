package com.samind.app.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordPieceTokenizerTest {

    // 0..3 specials, then subwords
    private val vocab = listOf(
        "[PAD]", "[UNK]", "[CLS]", "[SEP]",
        "skip", "meal", "##s", "star", "##ving", "thin", "!", "un",
    )

    private fun tokenizer(maxLen: Int = 16) =
        WordPieceTokenizer(vocab.asSequence(), maxLen)

    @Test
    fun encodesKnownWordsIntoSubwords() {
        val enc = tokenizer().encode("skip meals")
        val expected = intArrayOf(2, 4, 5, 6, 3) // [CLS] skip meal ##s [SEP]
        assertEquals(expected.toList(), enc.inputIds.take(5))
    }

    @Test
    fun greedyLongestMatchFirst() {
        val enc = tokenizer().encode("starving")
        assertEquals(listOf(2, 7, 8, 3), enc.inputIds.take(4)) // star + ##ving
    }

    @Test
    fun unknownWordBecomesUnk() {
        val enc = tokenizer().encode("zzz")
        assertEquals(listOf(2, 1, 3), enc.inputIds.take(3))
    }

    @Test
    fun punctuationIsItsOwnToken() {
        val enc = tokenizer().encode("thin!")
        assertEquals(listOf(2, 9, 10, 3), enc.inputIds.take(4))
    }

    @Test
    fun paddingAndMaskAgree() {
        val enc = tokenizer(maxLen = 8).encode("skip meals")
        assertEquals(8, enc.inputIds.size)
        assertEquals(8, enc.attentionMask.size)
        assertEquals(listOf(1, 1, 1, 1, 1, 0, 0, 0), enc.attentionMask.toList())
        assertTrue(enc.inputIds.drop(5).all { it == 0 })
    }

    @Test
    fun truncatesLongInputKeepingSep() {
        val enc = tokenizer(maxLen = 6).encode("skip meals skip meals skip meals")
        assertEquals(6, enc.attentionMask.count { it == 1 })
        assertEquals(3, enc.inputIds[5]) // [SEP] survives truncation
    }
}
