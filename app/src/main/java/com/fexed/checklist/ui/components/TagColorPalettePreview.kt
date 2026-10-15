package com.fexed.checklist.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.theme.ChecklistTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagColorPalettePreviewContent() {
    Surface {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TagColorPalette.forEachIndexed { index, color ->
                TagChip(tag = "Tag ${index + 1}", colorArgb = color)
            }
        }
    }
}

@Preview(name = "Tag Colors - Paper Light", showBackground = true)
@Composable
private fun TagColorPaletteLightPreview() {
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        TagColorPalettePreviewContent()
    }
}

@Preview(name = "Tag Colors - Parchment Dark", showBackground = true)
@Composable
private fun TagColorPaletteDarkPreview() {
    ChecklistTheme(themeMode = ThemeMode.DARK) {
        TagColorPalettePreviewContent()
    }
}
