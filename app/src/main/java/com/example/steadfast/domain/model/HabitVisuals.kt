package com.example.steadfast.domain.model

import com.example.steadfast.R

object HabitVisuals {
    val ICONS = listOf(
        "shield" to R.drawable.ic_habit_shield,
        "flame" to R.drawable.ic_habit_flame,
        "fitness" to R.drawable.ic_habit_fitness,
        "book" to R.drawable.ic_habit_book,
        "water" to R.drawable.ic_habit_water,
        "moon" to R.drawable.ic_habit_moon,
        "star" to R.drawable.ic_habit_star,
        "heart" to R.drawable.ic_habit_heart,
        "smile" to R.drawable.ic_habit_smile,
        "block" to R.drawable.ic_habit_block
    )

    fun getIconResource(iconName: String?): Int {
        return ICONS.firstOrNull { it.first == iconName }?.second ?: R.drawable.ic_habit_shield
    }

    val COLORS = listOf(
        0xFF4C662BL, // Brand Olive
        0xFF2E7D32L, // Emerald
        0xFF1565C0L, // Ocean Blue
        0xFF00838FL, // Cyan / Teal
        0xFF6A1B9AL, // Purple
        0xFFD84315L, // Sunset Amber
        0xFFC2185BL, // Coral / Rose
        0xFFF57F17L, // Gold
        0xFF455A64L  // Slate
    )
}
