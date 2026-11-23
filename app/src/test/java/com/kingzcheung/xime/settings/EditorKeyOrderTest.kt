package com.kingzcheung.xime.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorKeyOrderTest {
    @Test fun swapCutWithParagraphEndAndReopen() {
        val base = EditorKeyOrder.defaults
        val next = EditorKeyOrder.swap(base, base.indexOf("cut"), base.indexOf("end"))
        assertEquals("cut", next[2]); assertEquals("end", next[8])
        assertEquals(next, EditorKeyOrder.decode(next.joinToString(",")))
    }
    @Test fun missingDuplicateAndUnknownOrdersUseDefault() {
        val base = EditorKeyOrder.defaults
        for (raw in listOf(null, "", (base.dropLast(1) + "undo").joinToString(","), "new-action"))
            assertEquals(base, EditorKeyOrder.decode(raw))
    }
}
