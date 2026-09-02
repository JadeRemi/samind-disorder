package com.samind.app.ml

/**
 * Splits on-screen text into model-sized chunks.
 *
 * Scoring a whole screen as one blob lets unrelated UI labels dilute a real
 * trigger below the threshold (measured: 0.82 alone vs 0.55 mixed with chrome).
 * Chunking fixes that, but naive cutting can slice a phrase in half and hide it
 * from the classifier, so this implementation guarantees:
 *
 *  - sentences are never split while they fit in a chunk;
 *  - a sentence longer than the limit is windowed on word boundaries, never
 *    mid-word;
 *  - consecutive chunks overlap, so a phrase straddling a boundary still
 *    appears intact inside at least one chunk (the overlap is dropped only when
 *    it would push the chunk past [CHUNK_CHARS] — in that case the following
 *    sentence is whole anyway, so nothing needs protecting);
 *  - no chunk ever exceeds [CHUNK_CHARS], which is the model's input budget;
 *  - fragments from separate UI nodes are joined with sentence separators, so
 *    a caption and a button label never merge into one false sentence;
 *  - truncation (very text-heavy screens) is reported, never silent.
 */
object TextChunker {

    const val CHUNK_CHARS = 320
    const val OVERLAP_CHARS = 60
    const val MIN_CHUNK_CHARS = 12
    const val MAX_CHUNKS = 12

    data class Result(val chunks: List<String>, val truncated: Boolean)

    private val SENTENCE_SPLIT = Regex("(?<=[.!?…。！？])\\s+|[\\n\\r]+")
    private val WHITESPACE = Regex("\\s+")

    fun chunk(pieces: List<String>): Result {
        val sentences = pieces
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            // separate nodes are separate thoughts: keep them from fusing
            .joinToString("\n")
            .split(SENTENCE_SPLIT)
            .map { WHITESPACE.replace(it, " ").trim() }
            .filter { it.isNotEmpty() }
            .flatMap { window(it) }

        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        var truncated = false

        for (sentence in sentences) {
            val separator = if (current.isEmpty()) "" else " "
            if (current.isNotEmpty() &&
                current.length + separator.length + sentence.length > CHUNK_CHARS
            ) {
                chunks.add(current.toString())
                if (chunks.size >= MAX_CHUNKS) {
                    truncated = true
                    current = StringBuilder()
                    break
                }
                // carry the tail forward so a phrase spanning the cut survives whole,
                // but never past the chunk limit — the model input is bounded
                val tail = tailOf(chunks.last())
                current = if (tail.length + 1 + sentence.length <= CHUNK_CHARS) {
                    StringBuilder(tail)
                } else {
                    StringBuilder()
                }
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }

        if (current.isNotEmpty() && chunks.size < MAX_CHUNKS) chunks.add(current.toString())

        return Result(
            chunks = chunks.filter { it.length >= MIN_CHUNK_CHARS }.distinct(),
            truncated = truncated,
        )
    }

    /** Last [OVERLAP_CHARS] characters, cut at a word boundary. */
    private fun tailOf(text: String): String {
        if (text.length <= OVERLAP_CHARS) return text
        val tail = text.substring(text.length - OVERLAP_CHARS)
        val space = tail.indexOf(' ')
        return if (space in 0 until tail.length - 1) tail.substring(space + 1) else tail
    }

    /** Splits an over-long sentence into overlapping word-aligned windows. */
    private fun window(sentence: String): List<String> {
        if (sentence.length <= CHUNK_CHARS) return listOf(sentence)

        val windows = mutableListOf<String>()
        val words = sentence.split(' ').filter { it.isNotEmpty() }
        var current = StringBuilder()

        for (word in words) {
            if (current.isNotEmpty() && current.length + 1 + word.length > CHUNK_CHARS) {
                windows.add(current.toString())
                current = StringBuilder(tailOf(windows.last()))
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
        }
        if (current.isNotEmpty()) windows.add(current.toString())
        return windows
    }
}
