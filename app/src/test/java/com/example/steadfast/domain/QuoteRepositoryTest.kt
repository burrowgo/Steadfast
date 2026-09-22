package com.example.steadfast.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class QuoteRepositoryTest {

    private val sampleQuotes = listOf(
        Quote("General 1", null, "general"),
        Quote("General 2", null, "general"),
        Quote("General 3", null, "general"),
        Quote("Comeback 1", null, "comeback"),
        Quote("Comeback 2", null, "comeback")
    )

    private val repo = QuoteRepository(sampleQuotes)

    @Test
    fun `quote selection is deterministic for a given date and pool`() {
        val date1 = LocalDate.of(2026, 9, 21) // Day of year 264
        val q1 = repo.getQuoteForDay(isComeback = false, date = date1)
        val q2 = repo.getQuoteForDay(isComeback = false, date = date1)
        assertEquals(q1, q2)

        // Same date in comeback pool
        val cb1 = repo.getQuoteForDay(isComeback = true, date = date1)
        val cb2 = repo.getQuoteForDay(isComeback = true, date = date1)
        assertEquals(cb1, cb2)
        assertEquals("comeback", cb1.type)
    }

    @Test
    fun `different day of year picks according to pool size modulo`() {
        val date1 = LocalDate.of(2026, 1, 1) // Day 1
        val date2 = LocalDate.of(2026, 1, 2) // Day 2
        val date4 = LocalDate.of(2026, 1, 4) // Day 4 (1 mod 3 = 1)

        val q1 = repo.getQuoteForDay(isComeback = false, date = date1)
        val q2 = repo.getQuoteForDay(isComeback = false, date = date2)
        val q4 = repo.getQuoteForDay(isComeback = false, date = date4)

        assertNotEquals(q1, q2)
        assertEquals(q1, q4)
    }

    @Test
    fun `user offset shifts to next quote and wraps around`() {
        val date = LocalDate.of(2026, 9, 21)
        val q0 = repo.getQuoteForDay(isComeback = false, date = date, userOffset = 0)
        val q1 = repo.getQuoteForDay(isComeback = false, date = date, userOffset = 1)
        val q3 = repo.getQuoteForDay(isComeback = false, date = date, userOffset = 3)

        assertNotEquals(q0, q1)
        assertEquals(q0, q3) // 3 general quotes, offset 3 wraps around
    }

    @Test
    fun `periodic quote is stable within same hour slot`() {
        val slotStartMillis = 3600000L * 500L // Aligned to an hour boundary
        val q1 = repo.getPeriodicQuote(isComeback = false, nowMillis = slotStartMillis + 5 * 60 * 1000L) // +5m
        val q2 = repo.getPeriodicQuote(isComeback = false, nowMillis = slotStartMillis + 45 * 60 * 1000L) // +45m
        assertEquals(q1, q2)
    }

    @Test
    fun `periodic quote changes across different slots and respects user offset`() {
        val slotStartMillis = 3600000L * 500L
        val q1 = repo.getPeriodicQuote(isComeback = false, nowMillis = slotStartMillis, userOffset = 0)
        val q2 = repo.getPeriodicQuote(isComeback = false, nowMillis = slotStartMillis, userOffset = 1)
        val q3 = repo.getPeriodicQuote(isComeback = false, nowMillis = slotStartMillis, userOffset = 3) // wrap around

        assertNotEquals(q1, q2)
        assertEquals(q1, q3)
    }

    @Test
    fun `different seed modifiers generate different deterministic quotes`() {
        val slotStartMillis = 3600000L * 500L
        val qHabit1 = repo.getPeriodicQuote(isComeback = false, nowMillis = slotStartMillis, seedModifier = 1L)
        val qHabit2 = repo.getPeriodicQuote(isComeback = false, nowMillis = slotStartMillis, seedModifier = 2L)
        // Consistent across calls with same seedModifier
        val qHabit1Repeat = repo.getPeriodicQuote(isComeback = false, nowMillis = slotStartMillis, seedModifier = 1L)
        assertEquals(qHabit1, qHabit1Repeat)
        assertNotEquals(qHabit1, qHabit2)
    }
}
