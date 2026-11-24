package com.kingzcheung.xime.settings

/** The twelve slots surrounding the fixed direction pad, in row-major order. */
internal object EditorKeyOrder {
    const val PREF = "editor_action_key_order"
    val defaults = listOf("undo", "home", "end", "delete", "redo", "line_start", "line_end", "select_all",
        "cut", "copy", "paste", "enter")
    val actions = defaults + "forward_delete"
    fun decode(value: String?): List<String> = value?.split(',')?.takeIf {
        it.size == defaults.size && it.distinct().size == it.size && actions.containsAll(it)
    } ?: defaults

    fun replace(order: List<String>, slot: Int, action: String): List<String> {
        require(action in actions && action !in order)
        return order.toMutableList().apply { this[slot] = action }
    }

    fun swap(order: List<String>, from: Int, to: Int): List<String> = order.toMutableList().apply {
        this[from] = order[to]; this[to] = order[from]
    }
}
