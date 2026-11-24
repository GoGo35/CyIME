package com.kingzcheung.xime.service

import android.os.SystemClock
import com.kingzcheung.xime.ui.keyboard.KeyboardLayoutState
import com.kingzcheung.xime.util.FileLogger
import java.util.concurrent.atomic.AtomicReference

/** Debug log only. End at the first draw of the requested surface, not at an unrelated frame or
 * the engine callback. Does not measure GPU/display presentation. Holds no editor or input text. */
internal object SwitchFrameProbe {
    private val dispatchUp = ThreadLocal<Long>()
    private val latest = AtomicReference<Sample?>(null)

    fun onTouchUp(eventTime: Long, dispatch: () -> Boolean): Boolean {
        dispatchUp.set(eventTime)
        try { return dispatch() } finally { dispatchUp.remove() }
    }

    class Sample(val session: Long, val up: Long?, val queuedAt: Long) {
        var dequeuedAt = queuedAt
        var publishedAt = -1L
            private set
        private var schema: String? = null
        private var ascii = false
        private var layout: KeyboardLayoutState? = null
        // Publish and draw run on Main. A matching early preview and final confirmation count once.
        fun published(id: String, asciiMode: Boolean, surface: KeyboardLayoutState, now: Long = SystemClock.uptimeMillis()) {
            if (schema == id && ascii == asciiMode && layout == surface) return
            schema = id; ascii = asciiMode; layout = surface
            publishedAt = now
        }
        fun matches(editor: Long, id: String, asciiMode: Boolean, surface: KeyboardLayoutState) =
            editor == session && schema == id && ascii == asciiMode && layout == surface
    }

    fun begin(session: Long): Sample? {
        if (!FileLogger.isVerboseLoggingEnabled()) return null
        return Sample(session, dispatchUp.get(), SystemClock.uptimeMillis()).also { latest.set(it) }
    }

    fun discard(sample: Sample?) { if (sample != null) latest.compareAndSet(sample, null) }

    fun drawn(session: Long, schema: String, ascii: Boolean, surface: KeyboardLayoutState) {
        val sample = latest.get() ?: return
        if (!sample.matches(session, schema, ascii, surface) || !latest.compareAndSet(sample, null)) return
        val draw = SystemClock.uptimeMillis()
        FileLogger.d("SwitchFrame", "ascii=$ascii layout=$surface queueMs=${sample.dequeuedAt - sample.queuedAt} " +
            "prepareMs=${sample.publishedAt - sample.dequeuedAt} drawMs=${draw - sample.publishedAt} " +
            "requestToDrawMs=${draw - sample.queuedAt} upToDrawMs=${sample.up?.let { draw - it } ?: "n/a"}")
    }
}
