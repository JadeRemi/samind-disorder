package com.samind.app.ml

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun foldsLeetSubstitutions() {
        assertEquals("starving", TextNormalizer.normalize("st4rv1ng"))
    }

    @Test
    fun keepsRealNumbers() {
        assertEquals("cw 52 gw 44", TextNormalizer.normalize("cw 52 gw 44"))
        assertEquals("only 300 kcal today", TextNormalizer.normalize("only 300 kcal today"))
    }

    @Test
    fun removesSeparatorsInsideWords() {
        assertEquals("thin", TextNormalizer.normalize("t-h-i-n"))
    }

    @Test
    fun foldsCyrillicHomoglyphs() {
        assertEquals("purge", TextNormalizer.normalize("рurgе"))
    }

    @Test
    fun cyrillicWordsStayCyrillic() {
        assertEquals("привет", TextNormalizer.normalize("пpивет"))
        assertEquals("голодовка", TextNormalizer.normalize("г0лодовка"))
        assertEquals("что ты ешь", TextNormalizer.normalize("4то ты ешь"))
    }

    @Test
    fun collapsesStretchedLetters() {
        assertEquals("soo skinnyy", TextNormalizer.normalize("sooooo skinnyyyy"))
    }

    @Test
    fun leavesSafeTextAlone() {
        assertEquals("what a lovely sunset", TextNormalizer.normalize("what a lovely sunset"))
    }

    @Test
    fun readsEmojiWordSubstitutes() {
        assertEquals(
            "only star will give you the body",
            TextNormalizer.normalize("only ⭐ will give you the body"),
        )
        assertEquals("(star) star wing (wing)", TextNormalizer.normalize("(star)⭐️🪽(wing)"))
    }

    @Test
    fun stripsUnmappedEmoji() {
        assertEquals("great workout today", TextNormalizer.normalize("great workout 🎉 today"))
    }
}
