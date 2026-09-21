package com.example.steadfast.domain.model

data class Habit(
    val id: Long,
    val name: String,
    val icon: String = "shield",
    val color: Long = 0xFF4C662BL,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)
