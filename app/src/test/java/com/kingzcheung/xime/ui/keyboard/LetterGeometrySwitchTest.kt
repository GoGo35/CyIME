package com.kingzcheung.xime.ui.keyboard

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class LetterGeometrySwitchTest {
    @Test fun disablingCorrectionKeepsTheSameSwipeTargetsWithoutPublishingToTheEngine() {
        val updates = mutableListOf<String>()
        val chinese = LetterGeometry { updates.add(it) }
        val english = LetterGeometry(null)
        for ((i, key) in ('a'..'z').withIndex()) {
            val x = (i % 10).toFloat()
            val y = (i / 10).toFloat()
            val rect = Rect(x, y, x + 1f, y + 1f)
            chinese.place(key.toString(), rect)
            english.place(key.toString(), rect)
        }
        assertTrue(updates.last().isNotEmpty())
        assertEquals(26, english.snapshot().size)
        assertEquals(chinese.snapshot(), english.snapshot())
    }
}
