package com.fexed.checklist.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TagColorPaletteTest {
    @Test
    fun paletteContainsFourteenUniqueColors() {
        assertEquals(14, TagColorPalette.size)
        assertEquals(14, TagColorPalette.distinct().size)
    }
}
