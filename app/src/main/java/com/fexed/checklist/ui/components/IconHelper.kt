package com.fexed.checklist.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class ChecklistIcon(val id: String, val label: String, val category: String, val icon: ImageVector)

object IconHelper {
    val availableIcons = listOf(
        // General / Everyday
        ChecklistIcon("list", "General", "Everyday", Icons.Default.Checklist),
        ChecklistIcon("home", "Home & Cleaning", "Everyday", Icons.Default.Home),
        ChecklistIcon("shopping", "Shopping & Groceries", "Everyday", Icons.Default.ShoppingBag),
        ChecklistIcon("pets", "Pets & Care", "Everyday", Icons.Default.Pets),
        ChecklistIcon("cafe", "Coffee & Break", "Everyday", Icons.Default.LocalCafe),
        ChecklistIcon("cooking", "Cooking & Meal Prep", "Everyday", Icons.Default.Restaurant),

        // Travel & Mobility
        ChecklistIcon("trip", "Luggage & Trip", "Travel", Icons.Default.Luggage),
        ChecklistIcon("flight", "Flight & Airport", "Travel", Icons.Default.FlightTakeoff),
        ChecklistIcon("car", "Car & Vehicle", "Travel", Icons.Default.DirectionsCar),

        // Work & Study
        ChecklistIcon("work", "Work & Office", "Work", Icons.Default.Work),
        ChecklistIcon("business", "Business & Meeting", "Work", Icons.Default.BusinessCenter),
        ChecklistIcon("study", "Study & School", "Work", Icons.Default.School),
        ChecklistIcon("book", "Reading & Library", "Work", Icons.AutoMirrored.Filled.MenuBook),

        // Outdoors & Sports
        ChecklistIcon("forest", "Camping & Forest", "Outdoors", Icons.Default.Forest),
        ChecklistIcon("hiking", "Mountains & Hiking", "Outdoors", Icons.Default.Landscape),
        ChecklistIcon("cabin", "Cabin & Nature", "Outdoors", Icons.Default.Cabin),
        ChecklistIcon("bike", "Cycling & Bike", "Outdoors", Icons.AutoMirrored.Filled.DirectionsBike),
        ChecklistIcon("fitness", "Gym & Workout", "Outdoors", Icons.Default.FitnessCenter),

        // Gaming & Entertainment & Hobbies
        ChecklistIcon("gaming", "Video Games & Setup", "Gaming", Icons.Default.SportsEsports),
        ChecklistIcon("tabletop", "Board Games & RPG", "Gaming", Icons.Default.Casino),
        ChecklistIcon("photography", "Camera & Photography", "Hobby", Icons.Default.CameraAlt),
        ChecklistIcon("audio", "Audio & Music", "Hobby", Icons.Default.Headphones),
        ChecklistIcon("crafting", "Crafting & Tools", "Hobby", Icons.Default.Build)
    )

    fun getIcon(id: String): ImageVector {
        return availableIcons.find { it.id == id }?.icon ?: Icons.Default.Checklist
    }
}
