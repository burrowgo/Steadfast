package com.example.steadfast.domain

import androidx.annotation.StringRes
import com.example.steadfast.R

enum class RankTier {
    ENLISTED,
    OFFICER,
    GENERAL
}

data class Rank(
    val level: Int,
    @StringRes val nameRes: Int,
    val minDays: Int,
    val timeDescription: String,
    val tier: RankTier
)

data class RankProgress(
    val currentRank: Rank,
    val nextRank: Rank?,
    val daysToNextRank: Int,
    val progressToNext: Float
)
