package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import com.kingzcheung.xime.settings.EditorKeyOrder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material3.MaterialTheme
import com.kingzcheung.xime.ui.i18n.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.keyboard.KeyboardPanelLaunch
import com.kingzcheung.xime.settings.SettingsPreferences
import com.kingzcheung.xime.ui.keyboard.rememberFloatingGeometryLocked
import com.kingzcheung.xime.ui.keyboard.rememberInlineQuickActionEditing

internal fun keyboardPanelTitle(panel: KeyboardPanelLaunch.Panel) = when (panel) {
    KeyboardPanelLaunch.Panel.RESIZE -> "键盘尺寸与位置"
    KeyboardPanelLaunch.Panel.ARRANGE -> "工具栏与菜单排列"
}

@Composable
internal fun KeyboardPanelSettingsContent(panel: KeyboardPanelLaunch.Panel, onBack: () -> Unit) {
    KeyboardSettingsScaffold(keyboardPanelTitle(panel), onBack, preview = true, editPanel = panel) { modifier ->
        if (panel == KeyboardPanelLaunch.Panel.RESIZE) {
            val context = LocalContext.current
            SettingsToggleItem(Icons.TwoTone.Lock, "锁定悬浮键盘位置和大小",
                "锁定后不能移动或缩放，长按底部横条解锁",
                checked = rememberFloatingGeometryLocked(),
                onCheckedChange = { SettingsPreferences.setFloatingGeometryLocked(context, it) })
        }
        if (panel == KeyboardPanelLaunch.Panel.ARRANGE) {
            val context = LocalContext.current
            SettingsToggleItem(Icons.TwoTone.Edit, "在菜单里直接编辑",
                "在展开的菜单里直接调整按钮顺序和显示",
                checked = rememberInlineQuickActionEditing(),
                onCheckedChange = { SettingsPreferences.setInlineQuickActionEditing(context, it) })
        }
        Text(
            when (panel) {
                KeyboardPanelLaunch.Panel.RESIZE -> "点「调整尺寸」后拖动边框，调好点「确认」。"
                KeyboardPanelLaunch.Panel.ARRANGE -> "点「编辑排列」后拖动按钮，点勾号保存。"
            },
            modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (panel == KeyboardPanelLaunch.Panel.ARRANGE) EditorKeyOrderSettings()
    }
}

private val editorLabels = mapOf("undo" to "撤销", "home" to "段首", "end" to "段尾", "delete" to "删除",
    "redo" to "重做", "line_start" to "行首", "line_end" to "行尾", "select_all" to "全选",
    "cut" to "剪切", "copy" to "复制", "paste" to "粘贴", "enter" to "回车", "forward_delete" to "Del（向右删除）")

@Composable
private fun EditorKeyOrderSettings() {
    val prefs = SettingsPreferences.getPrefsPublic(LocalContext.current)
    var order by remember { mutableStateOf(EditorKeyOrder.decode(prefs.getString(EditorKeyOrder.PREF, null))) }
    var first by rememberSaveable { mutableIntStateOf(-1) }
    fun save(next: List<String>) {
        order = next
        prefs.edit().putString(EditorKeyOrder.PREF, next.joinToString(",")).apply()
        first = -1
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("编辑面板按键位置", style = MaterialTheme.typography.titleMedium)
        Text("依次点两个键即可互换，或选一个位置后点下方的备用键替换。方向盘固定；第二行靠近方向盘的两个位置只在宽屏显示。",
            style = MaterialTheme.typography.bodySmall)
        order.chunked(4).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                row.forEachIndexed { c, id ->
                    if (c == 2) Spacer(Modifier.width(16.dp))
                    val slot = 4 * r + c
                    Box(Modifier.weight(1f).heightIn(min = 48.dp).selectable(
                        selected = first == slot, role = Role.Button,
                        onClick = { if (first < 0) first = slot else save(EditorKeyOrder.swap(order, first, slot)) }
                    ), contentAlignment = Alignment.Center) {
                        Text(editorLabels.getValue(id), color = if (slot == first) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        (EditorKeyOrder.actions - order.toSet()).forEach { unused ->
            TextButton(enabled = first in order.indices, onClick = { save(EditorKeyOrder.replace(order, first, unused)) }) {
                Text("替换为：${editorLabels.getValue(unused)}")
            }
        }
        TextButton(onClick = { save(EditorKeyOrder.defaults) }, enabled = order != EditorKeyOrder.defaults || first >= 0) {
            Text("恢复默认位置")
        }
    }
}
