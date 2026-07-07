package com.samind.app.ml

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun foldsLeetSubstitutions() {
        assertEquals("starving", TextNormalizer.normalize("st4rv1ng"))
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
    fun collapsesStretchedLetters() {
        assertEquals("soo skinnyy", TextNormalizer.normalize("sooooo skinnyyyy"))
    }

    @Test
    fun leavesSafeTextAlone() {
        assertEquals("what a lovely sunset", TextNormalizer.normalize("what a lovely sunset"))
    }
}
