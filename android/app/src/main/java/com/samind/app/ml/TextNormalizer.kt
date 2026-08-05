package com.samind.app.ml

import java.text.Normalizer

// Mirror of samind_ml/normalize.py; keep the two in sync.
object TextNormalizer {

    private val leet = mapOf(
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's',
        '6' to 'g', '7' to 't', '8' to 'b', '9' to 'g',
        '@' to 'a', '$' to 's', '!' to 'i', '+' to 't',
        '€' to 'e', '£' to 'l', '¡' to 'i',
    )

    private val emojiWords = mapOf(
        "⭐" to " star ",
        "🌟" to " star ",
        "🪽" to " wing ",
        "🦋" to " butterfly ",
        "🪶" to " feather ",
        "👼" to " angel ",
        "💧" to " water ",
        "🍽" to " meal ",
        "🔥" to " burn ",
        "⚖" to " weight ",
        "🦴" to " bones ",
        "🚫" to " no ",
    )

    // applied only inside Latin-majority words
    private val homoglyphs = mapOf(
        'а' to 'a', 'е' to 'e', 'о' to 'o', 'р' to 'p', 'с' to 'c',
        'у' to 'y', 'х' to 'x', 'к' to 'k', 'в' to 'b', 'м' to 'm',
        'н' to 'h', 'т' to 't', 'і' to 'i', 'ѕ' to 's',
        'α' to 'a', 'β' to 'b', 'ε' to 'e', 'ο' to 'o', 'ρ' to 'p',
        'τ' to 't', 'υ' to 'u',
    )

    // the reverse direction for Cyrillic-majority words
    private val latinToCyrillic = mapOf(
        'a' to 'а', 'e' to 'е', 'o' to 'о', 'p' to 'р', 'c' to 'с',
        'y' to 'у', 'x' to 'х', 'k' to 'к', 'm' to 'м', 't' to 'т',
        'b' to 'в', 'h' to 'н',
    )

    private val cyrillicLeet = mapOf(
        '0' to 'о', '3' to 'з', '4' to 'ч', '6' to 'б',
    )

    private val zeroWidth = Regex("[\\u200B\\u200C\\u200D\\u2060\\uFEFF]")
    private val combining = Regex("[\\u0300-\\u036F]")
    private val separatedWord = Regex("\\b(?:\\w[.\\-_*·/\\\\|]){2,}\\w\\b")
    private val separators = Regex("[.\\-_*·/\\\\|]")
    private val repeats = Regex("(.)\\1{2,}")
    private val emoji = Regex("[\\x{1F000}-\\x{1FAFF}\\u2600-\\u27BF\\uFE0F]")
    private val spaces = Regex("\\s+")

    fun normalize(text: String): String {
        var s = Normalizer.normalize(text, Normalizer.Form.NFKD)
        s = zeroWidth.replace(s, "")
        s = combining.replace(s, "")
        s = s.lowercase()
        s = s.split(spaces).joinToString(" ") { foldWord(it) }
        s = separatedWord.replace(s) { m -> separators.replace(m.value, "") }
        for ((symbol, word) in emojiWords) s = s.replace(symbol, word)
        s = emoji.replace(s, " ")
        s = repeats.replace(s) { m -> m.groupValues[1].repeat(2) }
        return spaces.replace(s, " ").trim()
    }

    // fold toward the word's dominant script, so Russian text is never latinized
    private fun foldWord(word: String): String {
        val cyrillic = word.count { it in 'а'..'я' || it == 'ё' }
        val latin = word.count { it in 'a'..'z' }
        return if (cyrillic > 0 && cyrillic >= latin) {
            foldLeet(word.map { latinToCyrillic[it] ?: it }.joinToString(""), cyrillicLeet)
        } else {
            foldLeet(word.map { homoglyphs[it] ?: it }.joinToString(""), leet)
        }
    }

    // only inside words: "st4rving" folds, "cw 52 gw 44" keeps its numbers
    private fun foldLeet(s: String, table: Map<Char, Char>): String {
        val chars = s.toCharArray()
        for (i in chars.indices) {
            val mapped = table[chars[i]] ?: continue
            val prevAlpha = i > 0 && s[i - 1].isLetter()
            val nextAlpha = i + 1 < s.length && s[i + 1].isLetter()
            if (prevAlpha || nextAlpha) chars[i] = mapped
        }
        return String(chars)
    }
}
