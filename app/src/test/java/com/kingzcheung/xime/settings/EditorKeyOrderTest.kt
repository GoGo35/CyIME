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
    @Test fun replaceThenRestoreOneKeyWithoutLosingTheOtherSlots() {
        val base = EditorKeyOrder.swap(EditorKeyOrder.defaults, 0, 3)
        val changed = EditorKeyOrder.replace(base, 2, "forward_delete")
        assertEquals(changed, EditorKeyOrder.decode(changed.joinToString(",")))
        assertEquals(listOf("end"), EditorKeyOrder.actions - changed.toSet())
        assertEquals(base, EditorKeyOrder.replace(changed, 2, "end"))
    }
    @Test fun missingDuplicateAndUnknownOrdersUseDefault() {
        val base = EditorKeyOrder.defaults
        for (raw in listOf(null, "", (base.dropLast(1) + "undo").joinToString(","), "new-action"))
            assertEquals(base, EditorKeyOrder.decode(raw))
    }
}
