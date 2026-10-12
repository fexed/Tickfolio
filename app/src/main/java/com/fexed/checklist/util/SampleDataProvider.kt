package com.fexed.checklist.util

import com.fexed.checklist.data.local.ChecklistDao
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem

object SampleDataProvider {

    suspend fun populateSampleData(dao: ChecklistDao) {
        val now = System.currentTimeMillis()

        // 1. Business Trip Packing
        val tripChecklist = Checklist(
            title = "Work Trip to Zurich",
            description = "Packing & prep for the 4-day client conference",
            iconName = "trip",
            colorTag = 0xFF7C5C3BL,
            createdAt = now - 86400000L * 2,
            lastOpenedAt = now
        )
        val tripId = dao.insertChecklist(tripChecklist)
        dao.insertItems(
            listOf(
                ChecklistItem(
                    checklistId = tripId,
                    title = "Passport & ID",
                    description = "Keep in front pocket of backpack for security check",
                    tag = "Documents",
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = tripId,
                    title = "Work Laptop & Charger",
                    description = "USB-C 100W brick plus European wall adapter",
                    tag = "Tech",
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = tripId,
                    title = "Noise-cancelling headphones",
                    description = "Charge battery to 100% tonight",
                    tag = "Tech",
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = tripId,
                    title = "Presentation clicker / pointer",
                    description = "Spare AAA battery in side pouch",
                    tag = "Work",
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = tripId,
                    title = "Business casual shirts (3)",
                    description = "Ironed and placed in garment sleeve",
                    tag = "Clothing",
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = tripId,
                    title = "Toiletry bag (<100ml liquids)",
                    description = "Clear zip bag ready for airport TSA inspection",
                    tag = "Personal",
                    isCompleted = false
                )
            )
        )

        // 2. Car Maintenance Checklist
        val carChecklist = Checklist(
            title = "Car Maintenance & Seasonal Check",
            description = "Quarterly inspection and pre-roadtrip checklist",
            iconName = "car",
            colorTag = 0xFF5C6B52L,
            createdAt = now - 86400000L * 4,
            lastOpenedAt = now - 3600000L * 5
        )
        val carId = dao.insertChecklist(carChecklist)
        dao.insertItems(
            listOf(
                ChecklistItem(
                    checklistId = carId,
                    title = "Check Engine Oil Level",
                    description = "Warm up engine, wipe dipstick, ensure level is between min and max",
                    tag = "Engine",
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = carId,
                    title = "Tire Pressure & Tread Depth",
                    description = "2.3 bar front, 2.5 bar rear (check cold). Check spare tire too!",
                    tag = "Wheels",
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = carId,
                    title = "Top up Windshield Washer Fluid",
                    description = "Use winter-rated bug wash mix",
                    tag = "Fluids",
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = carId,
                    title = "Brake Pads & Fluid",
                    description = "Inspect reservoir level through transparent container",
                    tag = "Safety",
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = carId,
                    title = "Emergency Kit & First Aid",
                    description = "Reflective triangle, high-vis vests (2), torch, first aid kit expiration check",
                    tag = "Safety",
                    isCompleted = false
                )
            )
        )

        // 3. Apartment Cleaning Checklist
        val homeChecklist = Checklist(
            title = "Weekend Apartment Deep Clean",
            description = "Room-by-room tidy up and disinfection",
            iconName = "home",
            colorTag = 0xFF8C593EL,
            createdAt = now - 86400000L * 7,
            lastOpenedAt = now - 86400000L * 1
        )
        val homeId = dao.insertChecklist(homeChecklist)
        dao.insertItems(
            listOf(
                ChecklistItem(
                    checklistId = homeId,
                    title = "Kitchen: Clean stove and oven",
                    description = "Soak burner grates and wipe down grease splashes",
                    tag = "Kitchen",
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = homeId,
                    title = "Living Room: Dust bookshelves & TV unit",
                    description = "Use microfiber cloth with anti-static spray",
                    tag = "Living Room",
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = homeId,
                    title = "Bathroom: Descale shower head & polish tiles",
                    description = "Leave descaler on for 15 mins then rinse",
                    tag = "Bathroom",
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = homeId,
                    title = "Bedroom: Wash bed linens & flip mattress",
                    description = "60°C wash cycle for sheets and pillowcases",
                    tag = "Bedroom",
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = homeId,
                    title = "All Rooms: Vacuum & Mop floors",
                    description = "Wood cleaner for parquet, warm soapy water for tiles",
                    tag = "General",
                    isCompleted = false
                )
            )
        )
    }
}
