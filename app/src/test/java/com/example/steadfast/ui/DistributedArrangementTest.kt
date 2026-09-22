package com.example.steadfast.ui

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.steadfast.ui.home.distributedVerticalArrangement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistributedArrangementTest {

    private val density = Density(density = 2.0f, fontScale = 1.0f)

    @Test
    fun `empty sizes does nothing`() {
        val arrangement = distributedVerticalArrangement()
        val outPositions = IntArray(0)
        with(arrangement) {
            with(density) {
                arrange(totalSize = 1000, sizes = intArrayOf(), outPositions = outPositions)
            }
        }
    }

    @Test
    fun `single item is centered`() {
        val arrangement = distributedVerticalArrangement()
        val outPositions = IntArray(1)
        with(arrangement) {
            with(density) {
                arrange(totalSize = 1000, sizes = intArrayOf(400), outPositions = outPositions)
            }
        }
        assertEquals(300, outPositions[0])
    }

    @Test
    fun `fits with space-between when available is within min and max`() {
        val arrangement = distributedVerticalArrangement(minSpace = 10.dp, maxSpace = 50.dp)
        // 4 items of height 100. Total children = 400.
        // Total size = 520. Remaining = 120. 3 gaps = 40 each.
        // 40 is between minSpace (20px) and maxSpace (100px).
        val outPositions = IntArray(4)
        with(arrangement) {
            with(density) {
                arrange(
                    totalSize = 520,
                    sizes = intArrayOf(100, 100, 100, 100),
                    outPositions = outPositions
                )
            }
        }
        assertEquals(0, outPositions[0])
        assertEquals(140, outPositions[1])
        assertEquals(280, outPositions[2])
        assertEquals(420, outPositions[3])
        // Last item ends at 420 + 100 = 520, exactly totalSize!
        assertEquals(520, outPositions[3] + 100)
    }

    @Test
    fun `clamped to minSpace when space is constrained`() {
        val arrangement = distributedVerticalArrangement(minSpace = 10.dp, maxSpace = 50.dp)
        // minSpace = 10.dp = 20px at density 2.
        // 4 items of height 100. Total children = 400.
        // Total size = 410 (insufficient space).
        val outPositions = IntArray(4)
        with(arrangement) {
            with(density) {
                arrange(
                    totalSize = 410,
                    sizes = intArrayOf(100, 100, 100, 100),
                    outPositions = outPositions
                )
            }
        }
        // Should use minSpace (20px)
        assertEquals(0, outPositions[0])
        assertEquals(120, outPositions[1])
        assertEquals(240, outPositions[2])
        assertEquals(360, outPositions[3])
    }

    @Test
    fun `clamped to maxSpace and centered when screen is extra tall`() {
        val arrangement = distributedVerticalArrangement(minSpace = 10.dp, maxSpace = 40.dp)
        // maxSpace = 40.dp = 80px at density 2.
        // 4 items of height 100 = 400px.
        // Total size = 1000px. Remaining = 600px. Ideal gap = 200px > 80px.
        // Gap clamped to 80px. Total used = 400 + 3 * 80 = 640px.
        // Extra = 1000 - 640 = 360px. Offset = 180px.
        val outPositions = IntArray(4)
        with(arrangement) {
            with(density) {
                arrange(
                    totalSize = 1000,
                    sizes = intArrayOf(100, 100, 100, 100),
                    outPositions = outPositions
                )
            }
        }
        assertEquals(180, outPositions[0])
        assertEquals(180 + 100 + 80, outPositions[1])
        assertEquals(360 + 100 + 80, outPositions[2])
        assertEquals(540 + 100 + 80, outPositions[3])
        // Bottom margin = 1000 - (720 + 100) = 180px (equal to top offset!)
        assertEquals(1000, outPositions[3] + 100 + 180)
    }
}
