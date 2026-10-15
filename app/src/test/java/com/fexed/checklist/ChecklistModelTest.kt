package com.fexed.checklist

import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.data.preferences.StartPageOption
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.data.preferences.UserPreferences
import com.fexed.checklist.ui.screens.detail.DetailUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistModelTest {

    @Test
    fun checklistWithItems_computesProgressAndCountsCorrectly() {
        val checklist = Checklist(
            id = 1,
            title = "Trip Packing",
            description = "Packing for conference",
            iconName = "trip"
        )
        val items = listOf(
            ChecklistItem(id = 1, checklistId = 1, title = "Passport", tags = listOf("Docs"), isCompleted = true),
            ChecklistItem(id = 2, checklistId = 1, title = "Tickets", tags = listOf("Docs"), isCompleted = true),
            ChecklistItem(id = 3, checklistId = 1, title = "Laptop", tags = listOf("Tech"), isCompleted = false),
            ChecklistItem(id = 4, checklistId = 1, title = "Clothes", tags = listOf("Personal"), isCompleted = false)
        )

        val checklistWithItems = ChecklistWithItems(checklist = checklist, items = items)

        assertEquals(4, checklistWithItems.totalCount)
        assertEquals(2, checklistWithItems.completedCount)
        assertEquals(0.5f, checklistWithItems.progress, 0.001f)
        assertEquals(listOf("Docs", "Personal", "Tech"), checklistWithItems.tags)
    }

    @Test
    fun checklistWithItems_collectsMultipleTagsWithoutDuplicates() {
        val items = listOf(
            ChecklistItem(
                id = 1,
                checklistId = 1,
                title = "Pack phone",
                tags = listOf("Electronics", "Travel")
            ),
            ChecklistItem(
                id = 2,
                checklistId = 1,
                title = "Pack charger",
                tags = listOf("electronics", "Essentials")
            )
        )

        assertEquals(
            listOf("Electronics", "Essentials", "Travel"),
            ChecklistWithItems(Checklist(id = 1, title = "Trip"), items).tags
        )
    }

    @Test
    fun checklistWithItems_emptyList_hasZeroProgress() {
        val checklist = Checklist(id = 2, title = "Empty")
        val checklistWithItems = ChecklistWithItems(checklist = checklist, items = emptyList())

        assertEquals(0, checklistWithItems.totalCount)
        assertEquals(0, checklistWithItems.completedCount)
        assertEquals(0f, checklistWithItems.progress, 0.001f)
        assertTrue(checklistWithItems.tags.isEmpty())
    }

    @Test
    fun checklistWithItems_progressIncludesPartialAndRequiredChecks() {
        val checklist = Checklist(id = 3, title = "Buy supplies")
        val items = listOf(
            ChecklistItem(
                id = 1,
                checklistId = 3,
                title = "Buy 2 items",
                requiredChecks = 2,
                completedChecks = 1
            ),
            ChecklistItem(id = 2, checklistId = 3, title = "Pick up package", isCompleted = true)
        )

        val checklistWithItems = ChecklistWithItems(checklist, items)

        assertEquals(3, checklistWithItems.totalCount)
        assertEquals(2, checklistWithItems.completedCount)
        assertEquals(2f / 3f, checklistWithItems.progress, 0.001f)
        assertFalse(items.first().isCompleted)
    }

    @Test
    fun detailUiState_filterByTagAndQuery() {
        val checklist = Checklist(id = 1, title = "Apartment Cleaning")
        val items = listOf(
            ChecklistItem(
                id = 1,
                checklistId = 1,
                title = "Clean stove",
                tags = listOf("Kitchen", "Weekly"),
                isCompleted = false
            ),
            ChecklistItem(id = 2, checklistId = 1, title = "Mop kitchen floor", tags = listOf("Kitchen"), isCompleted = true),
            ChecklistItem(id = 3, checklistId = 1, title = "Dust living room", tags = listOf("Living Room"), isCompleted = false)
        )
        val cwi = ChecklistWithItems(checklist, items)

        // Filter by Tag "Kitchen"
        val stateTagOnly = DetailUiState(checklistWithItems = cwi, selectedTag = "Kitchen")
        assertEquals(2, stateTagOnly.filteredItems.size)
        assertEquals(
            listOf("Clean stove"),
            DetailUiState(checklistWithItems = cwi, selectedTag = "Weekly")
                .filteredItems.map { it.title }
        )

        // Filter by hideCompleted
        val stateHideCompleted = DetailUiState(checklistWithItems = cwi, selectedTag = "Kitchen", hideCompleted = true)
        assertEquals(1, stateHideCompleted.filteredItems.size)
        assertEquals("Clean stove", stateHideCompleted.filteredItems[0].title)

        // Filter by search query
        val stateSearch = DetailUiState(checklistWithItems = cwi, searchQuery = "dust")
        assertEquals(1, stateSearch.filteredItems.size)
        assertEquals("Dust living room", stateSearch.filteredItems[0].title)
    }

    @Test
    fun webMercatorProjection_conversionsAreAccurate() {
        val lat = 41.9028 // Rome
        val lon = 12.4964
        val zoom = 15

        val tileX = com.fexed.checklist.ui.components.WebMercatorProjection.lonToTileX(lon, zoom)
        val tileY = com.fexed.checklist.ui.components.WebMercatorProjection.latToTileY(lat, zoom)

        val revertedLon = com.fexed.checklist.ui.components.WebMercatorProjection.tileXToLon(tileX, zoom)
        val revertedLat = com.fexed.checklist.ui.components.WebMercatorProjection.tileYToLat(tileY, zoom)

        assertEquals(lon, revertedLon, 0.0001)
        assertEquals(lat, revertedLat, 0.0001)
    }

    @Test
    fun preferencesEnums_haveValidLabels() {
        assertEquals("All Checklists", StartPageOption.ALL_CHECKLISTS.label)
        assertEquals("Latest Opened", StartPageOption.LATEST_OPENED.label)
        assertEquals("Paper Light", ThemeMode.LIGHT.label)
        assertEquals("Parchment Dark", ThemeMode.DARK.label)
        assertFalse(
            UserPreferences(
                startPage = StartPageOption.ALL_CHECKLISTS,
                themeMode = ThemeMode.SYSTEM,
                lastOpenedChecklistId = null
            ).handwritingFontEnabled
        )
    }

    @Test
    fun checklist_withReminders_storesCorrectValues() {
        val now = System.currentTimeMillis()
        val checklist = Checklist(
            id = 10,
            title = "Hardware Store Visit",
            description = "Get tools and nails",
            iconName = "crafting",
            reminderTime = now + 7200000L,
            geofenceLat = 45.4642,
            geofenceLng = 9.1900,
            geofenceRadius = 350f,
            geofenceName = "Home Depot"
        )

        assertEquals("crafting", checklist.iconName)
        assertEquals(now + 7200000L, checklist.reminderTime)
        assertEquals(45.4642, checklist.geofenceLat!!, 0.0001)
        assertEquals(9.1900, checklist.geofenceLng!!, 0.0001)
        assertEquals(350f, checklist.geofenceRadius, 0.1f)
        assertEquals("Home Depot", checklist.geofenceName)
    }
}
