package com.example.steadfast.domain

import com.example.steadfast.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RankLadderTest {

    @Test
    fun `every threshold boundary produces exact rank`() {
        // Recruit (0-6)
        assertEquals(R.string.rank_recruit, RankLadder.getRankForDays(0).nameRes)
        assertEquals(R.string.rank_recruit, RankLadder.getRankForDays(6).nameRes)

        // Private (7-29)
        assertEquals(R.string.rank_private, RankLadder.getRankForDays(7).nameRes)
        assertEquals(R.string.rank_private, RankLadder.getRankForDays(29).nameRes)

        // Private First Class (30-59)
        assertEquals(R.string.rank_private_first_class, RankLadder.getRankForDays(30).nameRes)
        assertEquals(R.string.rank_private_first_class, RankLadder.getRankForDays(59).nameRes)

        // Corporal (60-89)
        assertEquals(R.string.rank_corporal, RankLadder.getRankForDays(60).nameRes)
        assertEquals(R.string.rank_corporal, RankLadder.getRankForDays(89).nameRes)

        // Sergeant (90-119)
        assertEquals(R.string.rank_sergeant, RankLadder.getRankForDays(90).nameRes)
        assertEquals(R.string.rank_sergeant, RankLadder.getRankForDays(119).nameRes)

        // Staff Sergeant (120-179)
        assertEquals(R.string.rank_staff_sergeant, RankLadder.getRankForDays(120).nameRes)
        assertEquals(R.string.rank_staff_sergeant, RankLadder.getRankForDays(179).nameRes)

        // Sergeant First Class (180-269)
        assertEquals(R.string.rank_sergeant_first_class, RankLadder.getRankForDays(180).nameRes)
        assertEquals(R.string.rank_sergeant_first_class, RankLadder.getRankForDays(269).nameRes)

        // Master Sergeant (270-364)
        assertEquals(R.string.rank_master_sergeant, RankLadder.getRankForDays(270).nameRes)
        assertEquals(R.string.rank_master_sergeant, RankLadder.getRankForDays(364).nameRes)

        // Sergeant Major (365-544)
        assertEquals(R.string.rank_sergeant_major, RankLadder.getRankForDays(365).nameRes)
        assertEquals(R.string.rank_sergeant_major, RankLadder.getRankForDays(544).nameRes)

        // Second Lieutenant (545-729)
        assertEquals(R.string.rank_second_lieutenant, RankLadder.getRankForDays(545).nameRes)
        assertEquals(R.string.rank_second_lieutenant, RankLadder.getRankForDays(729).nameRes)

        // First Lieutenant (730-1094)
        assertEquals(R.string.rank_first_lieutenant, RankLadder.getRankForDays(730).nameRes)
        assertEquals(R.string.rank_first_lieutenant, RankLadder.getRankForDays(1094).nameRes)

        // Captain (1095-1459)
        assertEquals(R.string.rank_captain, RankLadder.getRankForDays(1095).nameRes)
        assertEquals(R.string.rank_captain, RankLadder.getRankForDays(1459).nameRes)

        // Major (1460-1824)
        assertEquals(R.string.rank_major, RankLadder.getRankForDays(1460).nameRes)
        assertEquals(R.string.rank_major, RankLadder.getRankForDays(1824).nameRes)

        // Lieutenant Colonel (1825-2554)
        assertEquals(R.string.rank_lieutenant_colonel, RankLadder.getRankForDays(1825).nameRes)
        assertEquals(R.string.rank_lieutenant_colonel, RankLadder.getRankForDays(2554).nameRes)

        // Colonel (2555-3649)
        assertEquals(R.string.rank_colonel, RankLadder.getRankForDays(2555).nameRes)
        assertEquals(R.string.rank_colonel, RankLadder.getRankForDays(3649).nameRes)

        // Brigadier General (3650-5474)
        assertEquals(R.string.rank_brigadier_general, RankLadder.getRankForDays(3650).nameRes)
        assertEquals(R.string.rank_brigadier_general, RankLadder.getRankForDays(5474).nameRes)

        // Major General (5475-7299)
        assertEquals(R.string.rank_major_general, RankLadder.getRankForDays(5475).nameRes)
        assertEquals(R.string.rank_major_general, RankLadder.getRankForDays(7299).nameRes)

        // Lieutenant General (7300-9124)
        assertEquals(R.string.rank_lieutenant_general, RankLadder.getRankForDays(7300).nameRes)
        assertEquals(R.string.rank_lieutenant_general, RankLadder.getRankForDays(9124).nameRes)

        // General (9125-10949)
        assertEquals(R.string.rank_general, RankLadder.getRankForDays(9125).nameRes)
        assertEquals(R.string.rank_general, RankLadder.getRankForDays(10949).nameRes)

        // General of the Army (10950+)
        assertEquals(R.string.rank_general_of_the_army, RankLadder.getRankForDays(10950).nameRes)
        assertEquals(R.string.rank_general_of_the_army, RankLadder.getRankForDays(20000).nameRes)
    }

    @Test
    fun `negative days clamps to recruit`() {
        assertEquals(R.string.rank_recruit, RankLadder.getRankForDays(-10).nameRes)
    }

    @Test
    fun `progress to next rank calculations`() {
        // Day 0: Recruit, next is Private (7 days)
        val progress0 = RankLadder.getRankProgress(0)
        assertEquals(R.string.rank_recruit, progress0.currentRank.nameRes)
        assertEquals(R.string.rank_private, progress0.nextRank?.nameRes)
        assertEquals(7, progress0.daysToNextRank)
        assertEquals(0f, progress0.progressToNext, 0.001f)

        // Day 3 of 7: 3/7 progress
        val progress3 = RankLadder.getRankProgress(3)
        assertEquals(4, progress3.daysToNextRank)
        assertEquals(3f / 7f, progress3.progressToNext, 0.001f)

        // Day 7: Private (min 7), next is PFC (min 30), span = 23
        val progress7 = RankLadder.getRankProgress(7)
        assertEquals(R.string.rank_private, progress7.currentRank.nameRes)
        assertEquals(R.string.rank_private_first_class, progress7.nextRank?.nameRes)
        assertEquals(23, progress7.daysToNextRank)
        assertEquals(0f, progress7.progressToNext, 0.001f)
    }

    @Test
    fun `top rank has no next rank and 100 percent progress`() {
        val topProgress = RankLadder.getRankProgress(10950)
        assertEquals(R.string.rank_general_of_the_army, topProgress.currentRank.nameRes)
        assertNull(topProgress.nextRank)
        assertEquals(0, topProgress.daysToNextRank)
        assertEquals(1.0f, topProgress.progressToNext, 0.001f)
    }
}
