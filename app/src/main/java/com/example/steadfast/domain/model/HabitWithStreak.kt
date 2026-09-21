package com.example.steadfast.domain.model

import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.domain.Rank
import com.example.steadfast.domain.RankProgress

data class HabitWithStreak(
    val habit: Habit,
    val activeStreak: StreakEntity?,
    val currentStreakDays: Int,
    val rankProgress: RankProgress,
    val highestRankAchieved: Rank,
    val totalAttempts: Int
) {
    val currentRank: Rank get() = rankProgress.currentRank
}
