package com.kingzcheung.xime.settings

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SelectedEmailTest {
    @get:Rule val temp = TemporaryFolder()
    private fun ManualEmailRun.type(text: String) = text.forEach { key(it.toString()) { null } }

    @Test fun numberPanelThenDomainCountsOnceAndMixesWithManual() {
        val run = ManualEmailRun()
        val address = "abc123@163.com"
        run.type("abc"); run.type("123"); run.type("@")
        val picked = run.selectedDomain("@", "@163.com") { "这里$address" to "" }
        assertEquals(address, picked)
        EmailWordMemory.record(temp.root, picked!!)
        assertFalse(EnglishWordMemory.contains(temp.root, address))
        assertNull(run.key(" ") { error("already counted") })
        run.type(address)
        EmailWordMemory.record(temp.root, run.finish("") { address to "" }!!)
        assertTrue(EnglishWordMemory.contains(temp.root, address))
        assertTrue(EnglishWordMemory.forget(temp.root, address))
        assertFalse(EnglishWordMemory.contains(temp.root, address))
    }
    @Test fun differentAccountsAndAmbiguousFragmentsAreNeverMerged() {
        for (name in listOf("sssabc", "wfgbabc")) {
            val run = ManualEmailRun(); run.type("$name@16")
            val address = "$name@163.com"
            EmailWordMemory.record(temp.root, run.selectedDomain("@16", "@163.com") { address to "" }!!)
        }
        assertTrue(EnglishWordMemory.completions(temp.root, "ab", 10).isEmpty())
        val run = ManualEmailRun(); run.type("abc@")
        assertNull(run.selectedDomain("@", "@163.com") { "xabc@163.com" to "" })
        run.type("abc@1"); assertNull(run.selectedDomain("@1", "@163.com") { "abc@163.com" to ".uk" })
        run.type("abc＠1"); assertEquals("abc@163.com", run.selectedDomain("＠1", "@163.com") { "abc@163.com" to "" })
        assertNull(run.selectedDomain("@", "@163.com") { error("no manual local part") })
    }
}
