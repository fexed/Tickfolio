package com.fexed.checklist.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class TagListConverterTest {
    private val converter = TagListConverter()

    @Test
    fun serializesAndDeduplicatesMultipleTags() {
        val stored = converter.toStorage(listOf(" Travel ", "Kitchen", "travel", ""))

        assertEquals(listOf("Travel", "Kitchen"), converter.fromStorage(stored))
    }

    @Test
    fun readsLegacySingleTagValues() {
        assertEquals(listOf("Kitchen"), converter.fromStorage("Kitchen"))
        assertEquals(emptyList<String>(), converter.fromStorage(""))
    }
}
