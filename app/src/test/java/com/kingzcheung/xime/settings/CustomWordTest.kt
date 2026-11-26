package com.kingzcheung.xime.settings

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CustomWordTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun originalSelectionLearnsTheSpelling() {
        EnglishWordMemory.rememberSelected(temp.root, "cyime")
        assertEquals(listOf("cyime"), EnglishWordMemory.completions(temp.root, "cy", 8))
    }

    @Test fun selectingTheLastRememberedWordAlsoUndoesDemotion() {
        val dir = temp.root
        EnglishWordMemory.rememberSelected(dir, "cyime")
        assertTrue(EnglishWordMemory.demote(dir, "cyime"))
        EnglishWordMemory.rememberSelected(dir, "cyime")
        assertTrue(File(dir, EnglishWordMemory.DEMOTED_FILE_NAME).readLines().isEmpty())
        assertEquals(listOf("cyime"), File(dir, EnglishWordMemory.FILE_NAME).readLines())
    }

    @Test fun onlySecondCompletedManualAddressIsLearned() {
        val address = "abc123@188.xyz"
        val run = ManualEmailRun()
        repeat(2) { previous ->
            assertTrue(EnglishWordMemory.completions(temp.root, "abc", 8).isEmpty())
            address.forEach { c -> assertNull(run.key(c.toString()) { error("read prematurely") }) }
            val done = if (previous == 0) run.key(" ") { "$address " to "" } else run.finish("") { address to "" }
            assertEquals(address, done)
            EmailWordMemory.record(temp.root, done!!)
            assertNull(run.finish("") { address to "" })
        }
        assertEquals(listOf(address), EnglishWordMemory.completions(temp.root, "abc", 8))
    }

    @Test fun incompletePastedAndEditedAddressesAreNotCounted() {
        val run = ManualEmailRun()
        assertNull(run.key("abc123@188.xyz") { error("paste") })
        for (value in listOf("@example.org", "a@b", "a..b@example.org", "a@x..com")) {
            value.forEach { run.key(it.toString()) { error("not at boundary") } }
            assertNull(run.finish("") { value to "" })
        }
        "a@x.com".forEach { run.key(it.toString()) { null } }
        assertNull(run.finish("") { "a@x.com" to ".uk" })
    }

    @Test fun englishOnlyPackRoundTripsEnglishCapableFormats() {
        val english = listOf("about", "Team Spirit", "CyIME")
        val parsed = TxtCellDictionary.parse("'a'b'o'u't about\nTeam Spirit\nCyIME\n".toByteArray())
        assertEquals(english, parsed.english)
        // Other external IME formats are Chinese-only; CYIME, text, and Rime preserve English entries.
        for (format in listOf(DictionaryExport.Format.CYIME, DictionaryExport.Format.TEXT, DictionaryExport.Format.RIME)) {
            val output = DictionaryExport.write(format, "test", english.map { DictEntry(it, "") })
            assertEquals(format.name, 3, output.written)
            val result = if (format == DictionaryExport.Format.CYIME) JsonCellDictionary.parse(output.bytes) else {
                val file = File(temp.root, "out.${format.extension}").apply { writeBytes(output.bytes) }
                TxtCellDictionary.parse((DictionaryImportFormats.toText(file) ?: file).readBytes())
            }
            assertEquals(format.name, english, result.english)
        }
    }
}
