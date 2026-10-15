package com.fexed.checklist.data.local

import androidx.room.TypeConverter
import com.fexed.checklist.data.model.normalizeTags
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TagListConverter {
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun toStorage(tags: List<String>): String = gson.toJson(normalizeTags(tags))

    @TypeConverter
    fun fromStorage(value: String): List<String> {
        val storedValue = value.trim()
        if (storedValue.isEmpty()) return emptyList()
        if (!storedValue.startsWith("[")) return normalizeTags(listOf(storedValue))
        return normalizeTags(gson.fromJson<List<String>>(storedValue, listType).orEmpty())
    }
}
