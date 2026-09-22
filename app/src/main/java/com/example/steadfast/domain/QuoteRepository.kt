package com.example.steadfast.domain

import android.content.Context
import com.example.steadfast.R
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Clock
import java.time.LocalDate

class QuoteRepository(
    private val quotes: List<Quote>,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    val generalQuotes: List<Quote> = quotes.filter { it.type == "general" }
    val comebackQuotes: List<Quote> = quotes.filter { it.type == "comeback" }

    fun getQuoteForDay(isComeback: Boolean, date: LocalDate, userOffset: Int = 0): Quote {
        val pool = if (isComeback && comebackQuotes.isNotEmpty()) comebackQuotes else generalQuotes
        if (pool.isEmpty()) {
            return Quote(text = "Small days stack into big streaks.")
        }
        val dayOfYear = date.dayOfYear
        val baseIndex = (dayOfYear % pool.size + pool.size) % pool.size
        val effectiveIndex = ((baseIndex + userOffset) % pool.size + pool.size) % pool.size
        return pool[effectiveIndex]
    }

    fun getPeriodicQuote(
        isComeback: Boolean,
        nowMillis: Long = clock.millis(),
        userOffset: Int = 0,
        intervalMinutes: Long = 60L,
        seedModifier: Long = 0L
    ): Quote {
        val pool = if (isComeback && comebackQuotes.isNotEmpty()) comebackQuotes else generalQuotes
        if (pool.isEmpty()) {
            return Quote(text = "Small days stack into big streaks.")
        }
        val intervalMillis = (intervalMinutes * 60 * 1000L).coerceAtLeast(1000L)
        val timeSlot = nowMillis / intervalMillis
        // Deterministic pseudo-random seed per time slot, pool, and seedModifier (habitId)
        val seed = timeSlot xor (if (isComeback) 0x5DEECE66DL else 0xBL) xor (seedModifier * 0x5DEECE66DL + seedModifier)
        val slotRandomIndex = kotlin.random.Random(seed).nextInt(pool.size)
        val effectiveIndex = ((slotRandomIndex + userOffset) % pool.size + pool.size) % pool.size
        return pool[effectiveIndex]
    }

    fun getCurrentQuote(isComeback: Boolean, userOffset: Int = 0, seedModifier: Long = 0L): Quote {
        return getPeriodicQuote(isComeback, clock.millis(), userOffset, seedModifier = seedModifier)
    }

    companion object {
        fun loadFromRaw(context: Context, clock: Clock = Clock.systemDefaultZone()): QuoteRepository {
            val list = mutableListOf<Quote>()
            try {
                val inputStream = context.resources.openRawResource(R.raw.quotes)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.readText()
                reader.close()

                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val text = obj.getString("text")
                    val author = if (obj.has("author") && !obj.isNull("author")) obj.getString("author") else null
                    val type = if (obj.has("type")) obj.getString("type") else "general"
                    list.add(Quote(text, author, type))
                }
            } catch (e: Exception) {
                list.add(Quote(text = "Small days stack into big streaks."))
            }
            return QuoteRepository(list, clock)
        }
    }
}
