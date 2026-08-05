package com.samind.app.ml

// BERT-style WordPiece over already-normalized text. Behavior must match the
// HF tokenizer used in ml/samind_ml/train.py; verify with golden vectors
// whenever the vocab changes.
class WordPieceTokenizer(vocabLines: Sequence<String>, private val maxLen: Int = 128) {

    private val vocab = HashMap<String, Int>()

    init {
        var index = 0
        for (line in vocabLines) {
            val token = line.trimEnd('\n', '\r')
            if (token.isNotEmpty()) vocab[token] = index
            index++
        }
        require(vocab.containsKey(CLS) && vocab.containsKey(SEP) && vocab.containsKey(UNK)) {
            "vocab is missing special tokens"
        }
    }

    class Encoding(val inputIds: IntArray, val attentionMask: IntArray)

    fun encode(text: String): Encoding {
        val ids = ArrayList<Int>(maxLen)
        ids.add(vocab.getValue(CLS))
        outer@ for (word in basicSplit(text)) {
            for (pieceId in wordpiece(word)) {
                if (ids.size >= maxLen - 1) break@outer
                ids.add(pieceId)
            }
        }
        ids.add(vocab.getValue(SEP))

        val pad = vocab[PAD] ?: 0
        val inputIds = IntArray(maxLen) { pad }
        val mask = IntArray(maxLen)
        for (i in ids.indices) {
            inputIds[i] = ids[i]
            mask[i] = 1
        }
        return Encoding(inputIds, mask)
    }

    private fun basicSplit(text: String): List<String> {
        val words = ArrayList<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotEmpty()) {
                words.add(current.toString())
                current.clear()
            }
        }
        for (ch in text) {
            when {
                ch.isWhitespace() -> flush()
                !ch.isLetterOrDigit() -> {
                    flush()
                    words.add(ch.toString())
                }
                else -> current.append(ch)
            }
        }
        flush()
        return words
    }

    private fun wordpiece(word: String): List<Int> {
        if (word.length > MAX_WORD_CHARS) return listOf(vocab.getValue(UNK))
        val pieces = ArrayList<Int>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            var match: Int? = null
            while (start < end) {
                val candidate = (if (start > 0) "##" else "") + word.substring(start, end)
                val id = vocab[candidate]
                if (id != null) {
                    match = id
                    break
                }
                end--
            }
            if (match == null) return listOf(vocab.getValue(UNK))
            pieces.add(match)
            start = end
        }
        return pieces
    }

    companion object {
        const val CLS = "[CLS]"
        const val SEP = "[SEP]"
        const val UNK = "[UNK]"
        const val PAD = "[PAD]"
        private const val MAX_WORD_CHARS = 100
    }
}
