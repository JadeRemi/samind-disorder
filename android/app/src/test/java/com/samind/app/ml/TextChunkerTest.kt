package com.samind.app.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextChunkerTest {

    @Test
    fun shortScreenStaysOneChunk() {
        val result = TextChunker.chunk(listOf("Add contact", "skip meals wake up thinner", "Save"))
        assertEquals(1, result.chunks.size)
        assertFalse(result.truncated)
    }

    @Test
    fun sentencesAreNotSplitWhenTheyFit() {
        val long = "a".repeat(200)
        val sentence = "nothing tastes as good as skinny feels and everyone knows it."
        val result = TextChunker.chunk(listOf(long, sentence))
        // the sentence must appear complete inside at least one chunk
        assertTrue(
            result.chunks.any { it.contains(sentence) },
            )
    }

    @Test
    fun phraseSpanningTwoChunksSurvivesViaOverlap() {
        // fill a chunk almost exactly, then add the phrase so it straddles the cut
        val filler = List(6) { "Filler label number $it here to occupy space." }
        val pieces = filler + listOf("nothing tastes as good", "as skinny feels")
        val result = TextChunker.chunk(pieces)
        assertTrue(
            "no chunk kept the straddling phrase together: ${result.chunks}",
            result.chunks.any { it.contains("nothing tastes as good") && it.contains("as skinny feels") },
        )
    }

    @Test
    fun consecutiveChunksOverlap() {
        val pieces = List(20) { "Sentence number $it with enough words to take up room." }
        val result = TextChunker.chunk(pieces)
        assertTrue("expected several chunks", result.chunks.size >= 2)
        for (i in 1 until result.chunks.size) {
            val previousTail = result.chunks[i - 1].takeLast(TextChunker.OVERLAP_CHARS)
            val firstWord = result.chunks[i].substringBefore(' ')
            assertTrue(
                "chunk $i does not overlap its predecessor",
                previousTail.contains(firstWord),
            )
        }
    }

    @Test
    fun overLongSentenceIsWindowedOnWordBoundaries() {
        val words = List(200) { "word$it" }
        val result = TextChunker.chunk(listOf(words.joinToString(" ") + "."))
        assertTrue(result.chunks.size >= 2)
        for (chunk in result.chunks) {
            assertTrue("chunk exceeds the limit", chunk.length <= TextChunker.CHUNK_CHARS)
            // no word may be cut in half
            for (token in chunk.split(' ').filter { it.isNotEmpty() }) {
                assertTrue("word was sliced: $token", token.trimEnd('.') in words)
            }
        }
    }

    @Test
    fun separateNodesDoNotFuseIntoOneSentence() {
        // "Save" must not glue onto the caption and change its meaning
        val result = TextChunker.chunk(listOf("i ate too much today", "Save", "Discard"))
        assertTrue(result.chunks.first().contains("i ate too much today"))
    }

    @Test
    fun veryTextHeavyScreenReportsTruncation() {
        val pieces = List(400) { "Sentence number $it with plenty of extra words for volume." }
        val result = TextChunker.chunk(pieces)
        assertEquals(TextChunker.MAX_CHUNKS, result.chunks.size)
        assertTrue("truncation must be reported, never silent", result.truncated)
    }

    @Test
    fun tinyFragmentsAreDropped() {
        val result = TextChunker.chunk(listOf("OK", "Yes", "No"))
        assertTrue(result.chunks.isEmpty())
    }

    @Test
    fun noChunkEverExceedsTheModelInputLimit() {
        val mixed = listOf(
            "OK",
            "a".repeat(300) + ".",
            List(80) { "word$it" }.joinToString(" ") + ".",
            "short caption here",
            "b".repeat(310) + "!",
        ) + List(30) { "Sentence $it with a few more words to pack the buffer." }
        val result = TextChunker.chunk(mixed)
        for (chunk in result.chunks) {
            assertTrue(
                "chunk of ${chunk.length} chars exceeds ${TextChunker.CHUNK_CHARS}",
                chunk.length <= TextChunker.CHUNK_CHARS,
            )
        }
    }

    @Test
    fun emptyInputIsSafe() {
        val result = TextChunker.chunk(emptyList())
        assertTrue(result.chunks.isEmpty())
        assertFalse(result.truncated)
    }
}
