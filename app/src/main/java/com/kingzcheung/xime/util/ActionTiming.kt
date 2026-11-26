package com.kingzcheung.xime.util

import com.kingzcheung.xime.BuildConfig

/** One line per sampled action. Labels are constants, never key/candidate/editor text.
 * Owned by a single sequential coroutine, including its synchronous hops to Main. */
internal class ActionTiming private constructor(action: String) {
    private val start = System.nanoTime()
    private var previous = start
    private val phases = StringBuilder("rev=${BuildConfig.GIT_HASH} action=$action unit=us")

    fun mark(label: String) {
        val now = System.nanoTime()
        phases.append(' ').append(label).append('=').append((now - previous) / 1_000)
        previous = now
    }

    fun finish() {
        mark("tail")
        FileLogger.w("CyIME107Timing", phases.append(" total=").append((previous - start) / 1_000).toString())
    }

    companion object {
        fun begin(action: String): ActionTiming = ActionTiming(action)
    }
}
