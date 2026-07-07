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

    private val homoglyphs = mapOf(
        'а' to 'a', 'е' to 'e', 'о' to 'o', 'р' to 'p', 'с' to 'c',
        'у' to 'y', 'х' to 'x', 'к' to 'k', 'в' to 'b', 'м' to 'm',
        'н' to 'h', 'т' to 't', 'і' to 'i', 'ѕ' to 's',
        'α' to 'a', 'β' to 'b', 'ε' to 'e', 'ο' to 'o', 'ρ' to 'p',
        'τ' to 't', 'υ' to 'u',
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
        s = s.map { homoglyphs[it] ?: it }.joinToString("")
        s = s.map { leet[it] ?: it }.joinToString("")
        s = separatedWord.replace(s) { m -> separators.replace(m.value, "") }
        s = emoji.replace(s, " ")
        s = repeats.replace(s) { m -> m.groupValues[1].repeat(2) }
        return spaces.replace(s, " ").trim()
    }
}
