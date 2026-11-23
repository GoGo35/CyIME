package com.kingzcheung.xime.settings

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Only completed manual tokens are passed here. The pending file holds first sightings, not keystrokes. */
object EmailWordMemory {
    private const val FILE = "cyime_pending_emails.txt"
    private const val LABEL = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
    private val email = Regex("[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@(?:$LABEL\\.)+[A-Za-z]{2,63}")
    fun isValid(text: String) = text.length <= 254 && text.substringBefore('@').length <= 64 && email.matches(text)

    @Synchronized
    fun record(dir: File, text: String) {
        if (!isValid(text)) return
        val file = File(dir, FILE)
        val pending = if (file.isFile) file.readLines().toMutableSet() else linkedSetOf()
        if (pending.remove(text)) EnglishWordMemory.rememberSelected(dir, text) else pending.add(text)
        val tmp = File(dir, "$FILE.tmp")
        tmp.writeText(pending.toList().takeLast(2000).joinToString("\n"))
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

/** Tracks *only* accepted manual keys; editor text alone can never create a learned address. */
internal class ManualEmailRun {
    private var run = StringBuilder()
    private var tooLong = false
    private fun part(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in "._%+-@"
    fun reset() { run.setLength(0); tooLong = false }
    fun key(text: String, surrounding: (String) -> Pair<String, String>?): String? {
        if (text.length != 1) { reset(); return null }
        if (!part(text[0])) return finish(text, surrounding)
        if (run.length < 254) run.append(text) else tooLong = true
        return null
    }
    fun finish(separator: String, surrounding: (String) -> Pair<String, String>?): String? {
        val value = run.toString().takeIf { !tooLong && EmailWordMemory.isValid(it) }
        reset()
        if (value == null) return null
        val ending = value + separator
        val (before, after) = surrounding(ending) ?: return null
        if (!before.endsWith(ending) || before.dropLast(ending.length).lastOrNull()?.let(::part) == true) return null
        if (separator.isEmpty() && after.firstOrNull()?.let(::part) == true) return null
        return value
    }
}
