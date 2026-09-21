package com.example.steadfast.domain

import com.example.steadfast.R

object RankLadder {
    val ranks = listOf(
        Rank(0, R.string.rank_recruit, 0, "start", RankTier.ENLISTED),
        Rank(1, R.string.rank_private, 7, "1 week", RankTier.ENLISTED),
        Rank(2, R.string.rank_private_first_class, 30, "1 month", RankTier.ENLISTED),
        Rank(3, R.string.rank_corporal, 60, "2 months", RankTier.ENLISTED),
        Rank(4, R.string.rank_sergeant, 90, "3 months", RankTier.ENLISTED),
        Rank(5, R.string.rank_staff_sergeant, 120, "4 months", RankTier.ENLISTED),
        Rank(6, R.string.rank_sergeant_first_class, 180, "6 months", RankTier.ENLISTED),
        Rank(7, R.string.rank_master_sergeant, 270, "9 months", RankTier.ENLISTED),
        Rank(8, R.string.rank_sergeant_major, 365, "1 year", RankTier.ENLISTED),
        Rank(9, R.string.rank_second_lieutenant, 545, "18 months", RankTier.OFFICER),
        Rank(10, R.string.rank_first_lieutenant, 730, "2 years", RankTier.OFFICER),
        Rank(11, R.string.rank_captain, 1095, "3 years", RankTier.OFFICER),
        Rank(12, R.string.rank_major, 1460, "4 years", RankTier.OFFICER),
        Rank(13, R.string.rank_lieutenant_colonel, 1825, "5 years", RankTier.OFFICER),
        Rank(14, R.string.rank_colonel, 2555, "7 years", RankTier.OFFICER),
        Rank(15, R.string.rank_brigadier_general, 3650, "10 years", RankTier.GENERAL),
        Rank(16, R.string.rank_major_general, 5475, "15 years", RankTier.GENERAL),
        Rank(17, R.string.rank_lieutenant_general, 7300, "20 years", RankTier.GENERAL),
        Rank(18, R.string.rank_general, 9125, "25 years", RankTier.GENERAL),
        Rank(19, R.string.rank_general_of_the_army, 10950, "30 years", RankTier.GENERAL)
    )

    fun getRankForDays(streakDays: Int): Rank {
        val clampedDays = maxOf(0, streakDays)
        return ranks.lastOrNull { it.minDays <= clampedDays } ?: ranks.first()
    }

    fun getRankProgress(streakDays: Int): RankProgress {
        val clampedDays = maxOf(0, streakDays)
        val currentRank = getRankForDays(clampedDays)
        val currentIndex = ranks.indexOf(currentRank)
        val nextRank = if (currentIndex < ranks.lastIndex) ranks[currentIndex + 1] else null

        return if (nextRank == null) {
            RankProgress(
                currentRank = currentRank,
                nextRank = null,
                daysToNextRank = 0,
                progressToNext = 1.0f
            )
        } else {
            val totalSpan = nextRank.minDays - currentRank.minDays
            val progressDays = clampedDays - currentRank.minDays
            val fraction = if (totalSpan > 0) {
                (progressDays.toFloat() / totalSpan.toFloat()).coerceIn(0f, 1f)
            } else {
                1.0f
            }
            RankProgress(
                currentRank = currentRank,
                nextRank = nextRank,
                daysToNextRank = maxOf(0, nextRank.minDays - clampedDays),
                progressToNext = fraction
            )
        }
    }
}
