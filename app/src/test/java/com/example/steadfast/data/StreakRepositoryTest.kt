package com.example.steadfast.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.steadfast.data.db.AppDatabase
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.StreakCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class StreakRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: StreakRepository
    private var testClock: MutableClock = MutableClock(
        Instant.parse("2026-09-21T10:00:00Z"),
        ZoneId.of("UTC")
    )

    class MutableClock(
        private var instant: Instant,
        private val zone: ZoneId
    ) : Clock() {
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)
        override fun instant(): Instant = instant

        fun advanceDays(days: Long) {
            instant = instant.plusSeconds(days * 86400)
        }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StreakRepository(database.streakDao(), testClock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `start habit creates exactly one active run`() = runTest {
        repository.startHabit("Read book")

        val active = repository.getActiveStreak()
        assertNotNull(active)
        assertEquals("Read book", active?.habitName)
        assertNull(active?.endedAt)
        assertNull(active?.endDate)
        assertNull(active?.lengthDays)

        val history = repository.history.first()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `starting another habit closes previous and enforces at most one active run`() = runTest {
        repository.startHabit("First habit")
        testClock.advanceDays(5)
        repository.startHabit("Second habit")

        val active = repository.getActiveStreak()
        assertNotNull(active)
        assertEquals("Second habit", active?.habitName)

        val history = repository.history.first()
        assertEquals(1, history.size)
        assertEquals("First habit", history[0].habitName)
        assertEquals(5, history[0].lengthDays)
    }

    @Test
    fun `reset closes active run with correct length and reason and starts new run`() = runTest {
        repository.startHabit("Exercise")
        testClock.advanceDays(12)

        repository.resetStreak("Busy day")

        // Active run should now be a fresh run started on reset date (day 0)
        val active = repository.getActiveStreak()
        assertNotNull(active)
        assertEquals("Exercise", active?.habitName)
        assertEquals(0, StreakCalculator.streakDays(LocalDate.ofEpochDay(active!!.startDate), StreakCalculator.today(testClock)))

        // History should contain the ended run
        val history = repository.history.first()
        assertEquals(1, history.size)
        assertEquals("Exercise", history[0].habitName)
        assertEquals(12, history[0].lengthDays)
        assertEquals("Busy day", history[0].reason)
    }

    @Test
    fun `blank or whitespace reason is stored as null`() = runTest {
        repository.startHabit("Meditation")
        testClock.advanceDays(3)

        repository.resetStreak("   ")

        val history = repository.history.first()
        assertEquals(1, history.size)
        assertNull(history[0].reason)
    }

    @Test
    fun `undo restores previous active run and deletes the new run`() = runTest {
        repository.startHabit("Coding")
        testClock.advanceDays(8)

        repository.resetStreak("Travel")
        assertEquals(1, repository.history.first().size)

        // Now undo
        val undone = repository.undoLastReset()
        assertTrue(undone)

        val active = repository.getActiveStreak()
        assertNotNull(active)
        assertNull(active?.endedAt)
        assertNull(active?.endDate)
        assertNull(active?.lengthDays)

        // Today is still 8 days from start
        val count = StreakCalculator.streakDays(LocalDate.ofEpochDay(active!!.startDate), StreakCalculator.today(testClock))
        assertEquals(8, count)

        // History should be empty again
        val history = repository.history.first()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `editing reason on a historical run updates it`() = runTest {
        repository.startHabit("Drink water")
        testClock.advanceDays(4)
        repository.resetStreak("Initial reason")

        val historyItem = repository.history.first()[0]
        repository.updateReason(historyItem.id, "Updated reason after reflection")

        val updatedHistory = repository.history.first()
        assertEquals("Updated reason after reflection", updatedHistory[0].reason)
    }

    @Test
    fun `highest rank achieved is derived from longest streak across current and history`() = runTest {
        repository.startHabit("Study")
        testClock.advanceDays(35) // Achieved Private First Class (min 30 days)

        repository.resetStreak("Forgot")
        // Now on day 0 (Recruit)
        val stats = repository.statsFlow.first()
        assertEquals(35, stats.longestStreakDays)
        assertEquals(com.example.steadfast.R.string.rank_private_first_class, stats.highestRankAchieved.nameRes)
        assertEquals(2, stats.totalAttempts)
        assertEquals(2, stats.currentAttemptNumber)
    }

    @Test
    fun `updateActiveStartDate updates both startDate and startedAt`() = runTest {
        repository.startHabit("Cycling")
        val activeInitial = repository.getActiveStreak()!!
        val newStartDate = LocalDate.ofEpochDay(activeInitial.startDate).minusDays(3)

        repository.updateActiveStartDate(newStartDate)

        val activeUpdated = repository.getActiveStreak()!!
        assertEquals(newStartDate.toEpochDay(), activeUpdated.startDate)
        val expectedStartedAt = activeInitial.startedAt - (3 * 24 * 3600 * 1000L)
        assertEquals(expectedStartedAt, activeUpdated.startedAt)
    }
}
