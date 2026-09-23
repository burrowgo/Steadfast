package com.example.steadfast.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.steadfast.R
import com.example.steadfast.domain.Rank
import com.example.steadfast.domain.RankTier
import com.example.steadfast.ui.theme.LocalRankColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RankBadge(
    rank: Rank,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = LocalRankColors.current.accent
) {
    val rankName = stringResource(rank.nameRes)
    val contentDesc = stringResource(R.string.cd_rank_badge, rankName)
    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            when (rank.level) {
                0 -> drawRecruitShield(tint, w, h)
                1 -> drawChevrons(tint, count = 1, rockers = 0, w, h)
                2 -> drawChevrons(tint, count = 1, rockers = 1, w, h)
                3 -> drawChevrons(tint, count = 2, rockers = 0, w, h)
                4 -> drawChevrons(tint, count = 3, rockers = 0, w, h)
                5 -> drawChevrons(tint, count = 3, rockers = 1, w, h)
                6 -> drawChevrons(tint, count = 3, rockers = 2, w, h)
                7 -> drawChevrons(tint, count = 3, rockers = 3, w, h)
                8 -> drawChevrons(tint, count = 3, rockers = 3, w, h, hasStar = true)
                9 -> drawBars(tint, count = 1, w, h)
                10 -> drawBars(tint, count = 1, w, h, isSilver = true)
                11 -> drawBars(tint, count = 2, w, h)
                12 -> drawDiamondLeaf(tint, w, h)
                13 -> drawDiamondLeaf(tint, w, h, isSilver = true)
                14 -> drawEagleMark(tint, w, h)
                15 -> drawStars(tint, count = 1, w, h)
                16 -> drawStars(tint, count = 2, w, h)
                17 -> drawStars(tint, count = 3, w, h)
                18 -> drawStars(tint, count = 4, w, h)
                19 -> drawStars(tint, count = 5, w, h)
                else -> drawRecruitShield(tint, w, h)
            }
        }
    }
}

private fun DrawScope.drawRecruitShield(color: Color, w: Float, h: Float) {
    val strokeWidth = w * 0.08f
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.12f)
        lineTo(w * 0.85f, h * 0.28f)
        lineTo(w * 0.85f, h * 0.60f)
        cubicTo(w * 0.85f, h * 0.78f, w * 0.5f, h * 0.90f, w * 0.5f, h * 0.90f)
        cubicTo(w * 0.5f, h * 0.90f, w * 0.15f, h * 0.78f, w * 0.15f, h * 0.60f)
        lineTo(w * 0.15f, h * 0.28f)
        close()
    }
    drawPath(path, color, style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawChevrons(
    color: Color,
    count: Int,
    rockers: Int,
    w: Float,
    h: Float,
    hasStar: Boolean = false
) {
    val strokeWidth = w * 0.08f
    val chevronSpacing = h * 0.12f
    val startY = h * 0.20f

    // Draw upper chevrons pointing up
    for (i in 0 until count) {
        val y = startY + i * chevronSpacing
        val path = Path().apply {
            moveTo(w * 0.20f, y + h * 0.12f)
            lineTo(w * 0.50f, y)
            lineTo(w * 0.80f, y + h * 0.12f)
        }
        drawPath(path, color, style = Stroke(width = strokeWidth))
    }

    if (hasStar) {
        drawStar(color, Offset(w * 0.5f, h * 0.55f), w * 0.14f)
    }

    // Draw lower rockers (curved arcs)
    val rockerStartY = h * 0.68f
    for (i in 0 until rockers) {
        val y = rockerStartY + i * (h * 0.09f)
        val path = Path().apply {
            moveTo(w * 0.22f, y)
            quadraticTo(w * 0.50f, y + h * 0.10f, w * 0.78f, y)
        }
        drawPath(path, color, style = Stroke(width = strokeWidth))
    }
}

private fun DrawScope.drawBars(
    color: Color,
    count: Int,
    w: Float,
    h: Float,
    isSilver: Boolean = false
) {
    val barColor = if (isSilver) Color(0xFFC0C0C0) else color
    val barWidth = w * 0.14f
    val barHeight = h * 0.60f
    val top = h * 0.20f

    if (count == 1) {
        drawRoundRect(
            color = barColor,
            topLeft = Offset((w - barWidth) / 2f, top),
            size = Size(barWidth, barHeight)
        )
    } else {
        val spacing = w * 0.14f
        val left1 = (w - (barWidth * 2 + spacing)) / 2f
        val left2 = left1 + barWidth + spacing
        drawRoundRect(
            color = barColor,
            topLeft = Offset(left1, top),
            size = Size(barWidth, barHeight)
        )
        drawRoundRect(
            color = barColor,
            topLeft = Offset(left2, top),
            size = Size(barWidth, barHeight)
        )
    }
}

private fun DrawScope.drawDiamondLeaf(
    color: Color,
    w: Float,
    h: Float,
    isSilver: Boolean = false
) {
    val tintColor = if (isSilver) Color(0xFFDCDCDC) else color
    val path = Path().apply {
        moveTo(w * 0.50f, h * 0.15f)
        lineTo(w * 0.85f, h * 0.50f)
        lineTo(w * 0.50f, h * 0.85f)
        lineTo(w * 0.15f, h * 0.50f)
        close()
    }
    drawPath(path, tintColor, style = Fill)
    // Small inner accent
    val inner = Path().apply {
        moveTo(w * 0.50f, h * 0.25f)
        lineTo(w * 0.72f, h * 0.50f)
        lineTo(w * 0.50f, h * 0.75f)
        lineTo(w * 0.28f, h * 0.50f)
        close()
    }
    drawPath(inner, Color.White.copy(alpha = 0.4f), style = Fill)
}

private fun DrawScope.drawEagleMark(color: Color, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(w * 0.50f, h * 0.22f) // head
        lineTo(w * 0.88f, h * 0.35f) // wing right tip
        lineTo(w * 0.75f, h * 0.58f)
        lineTo(w * 0.58f, h * 0.52f)
        lineTo(w * 0.50f, h * 0.78f) // tail
        lineTo(w * 0.42f, h * 0.52f)
        lineTo(w * 0.25f, h * 0.58f)
        lineTo(w * 0.12f, h * 0.35f) // wing left tip
        close()
    }
    drawPath(path, color, style = Fill)
}

private fun DrawScope.drawStars(color: Color, count: Int, w: Float, h: Float) {
    val starRadius = when (count) {
        1 -> w * 0.35f
        2, 3 -> w * 0.18f
        else -> w * 0.14f
    }

    when (count) {
        1 -> drawStar(color, Offset(w * 0.5f, h * 0.5f), starRadius)
        2 -> {
            drawStar(color, Offset(w * 0.30f, h * 0.5f), starRadius)
            drawStar(color, Offset(w * 0.70f, h * 0.5f), starRadius)
        }
        3 -> {
            drawStar(color, Offset(w * 0.20f, h * 0.5f), starRadius)
            drawStar(color, Offset(w * 0.50f, h * 0.5f), starRadius)
            drawStar(color, Offset(w * 0.80f, h * 0.5f), starRadius)
        }
        4 -> {
            drawStar(color, Offset(w * 0.30f, h * 0.35f), starRadius)
            drawStar(color, Offset(w * 0.70f, h * 0.35f), starRadius)
            drawStar(color, Offset(w * 0.30f, h * 0.65f), starRadius)
            drawStar(color, Offset(w * 0.70f, h * 0.65f), starRadius)
        }
        5 -> {
            // Pentagon circle
            val center = Offset(w * 0.5f, h * 0.5f)
            val circleRadius = w * 0.28f
            for (i in 0 until 5) {
                val angle = (i * 72.0 - 90.0) * PI / 180.0
                val pos = Offset(
                    (center.x + circleRadius * cos(angle)).toFloat(),
                    (center.y + circleRadius * sin(angle)).toFloat()
                )
                drawStar(color, pos, starRadius * 0.75f)
            }
        }
    }
}

private fun DrawScope.drawStar(color: Color, center: Offset, outerRadius: Float) {
    val innerRadius = outerRadius * 0.40f
    val path = Path()
    for (i in 0 until 10) {
        val angle = (i * 36.0 - 90.0) * PI / 180.0
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = Fill)
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "RankBadges")
@Composable
private fun RankBadgePreview() {
    com.example.steadfast.ui.theme.SteadfastTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            RankBadge(com.example.steadfast.domain.RankLadder.ranks[0])
            RankBadge(com.example.steadfast.domain.RankLadder.ranks[1])
            RankBadge(com.example.steadfast.domain.RankLadder.ranks[4])
            RankBadge(com.example.steadfast.domain.RankLadder.ranks[11])
            RankBadge(com.example.steadfast.domain.RankLadder.ranks[19])
        }
    }
}
