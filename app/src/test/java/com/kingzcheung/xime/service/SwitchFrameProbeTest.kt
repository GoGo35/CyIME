package com.kingzcheung.xime.service

import com.kingzcheung.xime.ui.keyboard.KeyboardLayoutState
import org.junit.Assert.*
import org.junit.Test

class SwitchFrameProbeTest {
    @Test fun measureOnlyTheRequestedLayoutInTheSameEditorFromFirstPublication() {
        val sample = SwitchFrameProbe.Sample(session = 7L, up = 100L, queuedAt = 101L)
        val surface = KeyboardLayoutState.English
        assertFalse(sample.matches(7L, "rime_ice", true, surface))
        sample.published("rime_ice", true, surface, now = 111L)
        assertFalse(sample.matches(8L, "rime_ice", true, surface))
        assertFalse(sample.matches(7L, "rime_ice", true, KeyboardLayoutState.Number))
        assertFalse(sample.matches(7L, "rime_ice", false, surface))
        assertFalse(sample.matches(7L, "another", true, surface))
        assertTrue(sample.matches(7L, "rime_ice", true, surface))
        sample.published("rime_ice", true, surface, now = 131L)
        assertEquals(111L, sample.publishedAt)
        sample.published("cyime_english", false, KeyboardLayoutState.Chinese, now = 142L)
        assertEquals(142L, sample.publishedAt)
        assertFalse(sample.matches(7L, "rime_ice", true, surface))
    }
}
