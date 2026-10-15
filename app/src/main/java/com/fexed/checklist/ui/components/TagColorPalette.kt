package com.fexed.checklist.ui.components

import com.fexed.checklist.data.model.tagKey
import com.fexed.checklist.data.preferences.TagColorRecord

val TagColorPalette = listOf(
    0xFF795548.toInt(),
    0xFF557A55.toInt(),
    0xFFB85C38.toInt(),
    0xFF8A6D3B.toInt(),
    0xFF4E6A89.toInt(),
    0xFF775477.toInt(),
    0xFF36796F.toInt(),
    0xFF9A5360.toInt(),
    0xFF6F7040.toInt(),
    0xFF4F7176.toInt(),
    0xFFA24E3F.toInt(),
    0xFF5B6D8A.toInt(),
    0xFF80649A.toInt(),
    0xFF8A7032.toInt()
)

fun tagColorFor(tag: String, tagColors: Map<String, TagColorRecord>): Int {
    val key = tagKey(tag)
    return tagColors[key]?.colorArgb
        ?: TagColorPalette[Math.floorMod(key.hashCode(), TagColorPalette.size)]
}
