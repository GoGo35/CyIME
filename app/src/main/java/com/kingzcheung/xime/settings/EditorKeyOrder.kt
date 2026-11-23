package com.kingzcheung.xime.settings

/** The twelve slots surrounding the fixed direction pad, in row-major order. */
internal object EditorKeyOrder {
    const val PREF = "editor_action_key_order"
    val defaults = listOf("undo", "home", "end", "delete", "redo", "line_start", "line_end", "select_all",
        "cut", "copy", "paste", "enter")
    fun decode(value: String?): List<String> = value?.split(',')?.takeIf {
        it.size == defaults.size && it.toSet() == defaults.toSet()
    } ?: defaults

    fun swap(order: List<String>, from: Int, to: Int): List<String> = order.toMutableList().apply {
        this[from] = order[to]; this[to] = order[from]
    }
}
