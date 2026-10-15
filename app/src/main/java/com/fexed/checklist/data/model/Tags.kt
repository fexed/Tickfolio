package com.fexed.checklist.data.model

import java.util.Locale

fun normalizeTags(tags: List<String>): List<String> {
    val uniqueTags = linkedMapOf<String, String>()
    tags.forEach { rawTag ->
        val tag = rawTag.trim()
        if (tag.isNotEmpty()) {
            uniqueTags.putIfAbsent(tag.lowercase(Locale.ROOT), tag)
        }
    }
    return uniqueTags.values.toList()
}

fun tagKey(tag: String): String = tag.trim().lowercase(Locale.ROOT)
