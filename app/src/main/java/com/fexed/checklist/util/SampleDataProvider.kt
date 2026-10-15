package com.fexed.checklist.util

import com.fexed.checklist.data.local.ChecklistDao
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.ChecklistWithItems

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
                    tags = listOf("Documents"),
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = tripId,
                    title = "Work Laptop & Charger",
                    description = "USB-C 100W brick plus European wall adapter",
                    tags = listOf("Tech"),
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = tripId,
                    title = "Noise-cancelling headphones",
                    description = "Charge battery to 100% tonight",
                    tags = listOf("Tech"),
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = tripId,
                    title = "Presentation clicker / pointer",
                    description = "Spare AAA battery in side pouch",
                    tags = listOf("Work"),
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = tripId,
                    title = "Business casual shirts (3)",
                    description = "Ironed and placed in garment sleeve",
                    tags = listOf("Clothing"),
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = tripId,
                    title = "Toiletry bag (<100ml liquids)",
                    description = "Clear zip bag ready for airport TSA inspection",
                    tags = listOf("Personal"),
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
                    tags = listOf("Engine"),
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = carId,
                    title = "Tire Pressure & Tread Depth",
                    description = "2.3 bar front, 2.5 bar rear (check cold). Check spare tire too!",
                    tags = listOf("Wheels"),
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = carId,
                    title = "Top up Windshield Washer Fluid",
                    description = "Use winter-rated bug wash mix",
                    tags = listOf("Fluids"),
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = carId,
                    title = "Brake Pads & Fluid",
                    description = "Inspect reservoir level through transparent container",
                    tags = listOf("Safety"),
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = carId,
                    title = "Emergency Kit & First Aid",
                    description = "Reflective triangle, high-vis vests (2), torch, first aid kit expiration check",
                    tags = listOf("Safety"),
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
                    tags = listOf("Kitchen"),
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = homeId,
                    title = "Living Room: Dust bookshelves & TV unit",
                    description = "Use microfiber cloth with anti-static spray",
                    tags = listOf("Living Room"),
                    isCompleted = true
                ),
                ChecklistItem(
                    checklistId = homeId,
                    title = "Bathroom: Descale shower head & polish tiles",
                    description = "Leave descaler on for 15 mins then rinse",
                    tags = listOf("Bathroom"),
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = homeId,
                    title = "Bedroom: Wash bed linens & flip mattress",
                    description = "60°C wash cycle for sheets and pillowcases",
                    tags = listOf("Bedroom"),
                    isCompleted = false
                ),
                ChecklistItem(
                    checklistId = homeId,
                    title = "All Rooms: Vacuum & Mop floors",
                    description = "Wood cleaner for parquet, warm soapy water for tiles",
                    tags = listOf("General"),
                    isCompleted = false
                )
            )
        )

        defaultTemplates(now).forEach { template ->
            val checklistId = dao.insertChecklist(template.checklist)
            dao.insertItems(template.items.map { it.copy(checklistId = checklistId) })
        }
    }

    internal fun defaultTemplates(now: Long): List<ChecklistWithItems> = listOf(
        template(
            "Weekend Trip",
            "Pack the essentials and get your home ready before heading out.",
            "trip", 0xFF7C5C3BL, now,
            listOf(
                "Check travel documents and booking details" to "Documents",
                "Pack outfits, underwear, and sleepwear" to "Clothing",
                "Pack toiletries and personal essentials" to "Personal",
                "Pack phone charger and any required adapters" to "Tech",
                "Download tickets, directions, and offline maps" to "Travel",
                "Check windows, appliances, and door locks before leaving" to "Home"
            )
        ),
        template(
            "Weekly Home Reset",
            "A room-by-room refresh to start the week with a tidy space.",
            "home", 0xFF8C593EL, now,
            listOf(
                "Put away items left on tables and counters" to "General",
                "Wipe kitchen counters and clean the sink" to "Kitchen",
                "Clean bathroom surfaces and mirrors" to "Bathroom",
                "Change towels and bed linens" to "Laundry",
                "Dust surfaces and vacuum floors" to "General",
                "Empty bins and sort recycling" to "Waste"
            )
        ),
        template(
            "Grocery Run",
            "Plan meals and pick up what you need without buying duplicates.",
            "shopping", 0xFF5C6B52L, now,
            listOf(
                "Check the fridge, freezer, and pantry" to "Planning",
                "Choose meals for the next few days" to "Planning",
                "Add fruit and vegetables to your shopping list" to "Produce",
                "Add proteins and chilled essentials" to "Chilled",
                "Add pantry staples and snacks" to "Pantry",
                "Check household supplies and bring reusable bags" to "Household"
            )
        ),
        template(
            "Car Trip Preparation",
            "Review your vehicle and travel essentials before a longer drive.",
            "car", 0xFF5C6B52L, now,
            listOf(
                "Check tire pressure against the vehicle's recommended values" to "Wheels",
                "Check fluid levels following the owner's manual" to "Fluids",
                "Check lights, indicators, and wipers" to "Visibility",
                "Review warning lights and any maintenance due" to "Maintenance",
                "Check required documents and emergency supplies" to "Essentials",
                "Plan the route, rest stops, and fuel or charging stops" to "Travel"
            )
        ),
        template(
            "Productive Workday",
            "Start with clear priorities and finish with a clean handover to tomorrow.",
            "work", 0xFF7C5C3BL, now,
            listOf(
                "Review today's calendar and deadlines" to "Planning",
                "Choose your three most important tasks" to "Priorities",
                "Gather the notes and files needed for your first task" to "Preparation",
                "Set aside a focused work session" to "Focus",
                "Review messages and follow up on outstanding requests" to "Communication",
                "Record progress and write tomorrow's next steps" to "Wrap-up"
            )
        ),
        template(
            "Moving Home",
            "Keep the practical details of a move in one place.",
            "home", 0xFF8C593EL, now,
            listOf(
                "Confirm moving dates, access, and transport" to "Planning",
                "Sort belongings to keep, donate, or recycle" to "Sorting",
                "Label boxes by room and contents" to "Packing",
                "Pack a separate bag of first-night essentials" to "Essentials",
                "Arrange utility changes and update relevant addresses" to "Admin",
                "Photograph meter readings and property condition where applicable" to "Handover",
                "Check every room and return or collect keys" to "Handover"
            )
        ),
        template(
            "Laundry Day",
            "Work through laundry loads from washing to putting everything away.",
            "home", 0xFF8C593EL, now,
            listOf(
                "Wash laundry loads" to "Washing",
                "Dry laundry loads" to "Drying",
                "Fold and put away laundry loads" to "Putting Away",
                "Refresh towels and linens" to "Linens",
                "Clean the lint filter and laundry area" to "Maintenance"
            ),
            multiCheckCounts = mapOf(
                "Wash laundry loads" to 3,
                "Dry laundry loads" to 3,
                "Fold and put away laundry loads" to 3
            )
        ),
        template(
            "Hosting Friends",
            "Get the shared space and refreshments ready for guests.",
            "cooking", 0xFF7C5C3BL, now,
            listOf(
                "Set out chairs" to "Setup",
                "Prepare place settings" to "Setup",
                "Plan food and drinks" to "Food",
                "Tidy the shared spaces" to "Home",
                "Set out serving dishes and utensils" to "Setup",
                "Check guest arrival details" to "Planning"
            ),
            multiCheckCounts = mapOf(
                "Set out chairs" to 6,
                "Prepare place settings" to 6
            )
        )
    )

    private fun template(
        title: String,
        description: String,
        iconName: String,
        colorTag: Long,
        now: Long,
        items: List<Pair<String, String>>,
        multiCheckCounts: Map<String, Int> = emptyMap()
    ) = ChecklistWithItems(
        checklist = Checklist(
            title = title,
            description = description,
            iconName = iconName,
            colorTag = colorTag,
            createdAt = now,
            lastOpenedAt = now
        ),
        items = items.mapIndexed { position, (itemTitle, tag) ->
            ChecklistItem(
                checklistId = 0,
                title = itemTitle,
                tags = listOf(tag),
                isCompleted = false,
                requiredChecks = multiCheckCounts[itemTitle] ?: 1,
                completedChecks = 0,
                position = position,
                createdAt = now
            )
        }
    )
}
