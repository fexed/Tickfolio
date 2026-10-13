package com.fexed.checklist

import com.fexed.checklist.util.SampleDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleDataProviderTest {
    @Test
    fun defaultTemplates_includeAllDraftsInOrder() {
        val templates = SampleDataProvider.defaultTemplates(1234L)

        assertEquals(
            listOf(
                "Weekend Trip", "Weekly Home Reset", "Grocery Run",
                "Car Trip Preparation", "Productive Workday", "Moving Home",
                "Laundry Day", "Hosting Friends"
            ),
            templates.map { it.checklist.title }
        )
        assertEquals(listOf(6, 6, 6, 6, 6, 7, 5, 6), templates.map { it.items.size })
        assertEquals(
            listOf("trip", "home", "shopping", "car", "work", "home", "home", "cooking"),
            templates.map { it.checklist.iconName }
        )

        assertEquals(
            mapOf(
                "Laundry Day" to mapOf(
                    "Wash laundry loads" to 3,
                    "Dry laundry loads" to 3,
                    "Fold and put away laundry loads" to 3
                ),
                "Hosting Friends" to mapOf(
                    "Set out chairs" to 6,
                    "Prepare place settings" to 6
                )
            ),
            templates.associate { template ->
                template.checklist.title to template.items
                    .filter { it.requiredChecks > 1 }
                    .associate { it.title to it.requiredChecks }
            }.filterValues { it.isNotEmpty() }
        )
    }

    @Test
    fun defaultTemplates_areUncheckedOrderedAndHaveNoReminders() {
        SampleDataProvider.defaultTemplates(1234L).forEach { template ->
            assertTrue(template.checklist.description.isNotBlank())
            assertEquals(1234L, template.checklist.createdAt)
            assertNull(template.checklist.reminderTime)
            assertNull(template.checklist.geofenceLat)
            assertNull(template.checklist.geofenceLng)
            assertEquals(template.items.indices.toList(), template.items.map { it.position })
            template.items.forEach { item ->
                assertTrue(item.title.isNotBlank())
                assertTrue(item.tag.isNotBlank())
                assertFalse(item.isCompleted)
                assertEquals(0, item.completedChecks)
                assertTrue(item.requiredChecks >= 1)
                assertEquals(1234L, item.createdAt)
            }
        }
    }
}
