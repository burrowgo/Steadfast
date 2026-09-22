package com.example.steadfast.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.steadfast.R
import com.example.steadfast.ui.theme.BarlowCondensed
import com.example.steadfast.ui.theme.LocalRankColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DayCounter(
    days: Int,
    progressToNext: Float,
    startedAtMillis: Long = 0L,
    modifier: Modifier = Modifier,
    size: Dp = 152.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressToNext.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "rankProgress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val accentColor = LocalRankColors.current.accent
    val daysContentDesc = stringResource(R.string.cd_streak_counter, days)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics { contentDescription = daysContentDesc }
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 7.dp.toPx()
                val paddingPx = strokeWidth / 2f + 2.dp.toPx()
                val diameter = this.size.minDimension - 2f * paddingPx
                val topLeft = Offset(
                    (this.size.width - diameter) / 2f,
                    (this.size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)
                val centerOffset = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)

                // 1. Subtle hairline inner guide bezel
                drawCircle(
                    color = trackColor.copy(alpha = 0.22f),
                    radius = (diameter - strokeWidth) / 2f - 2.5.dp.toPx(),
                    center = centerOffset,
                    style = Stroke(width = 1.dp.toPx())
                )

                // 2. Background track
                drawArc(
                    color = trackColor.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 3. Animated Progress arc with linear gradient
                if (animatedProgress > 0f) {
                    val sweepAngle = animatedProgress * 360f
                    val arcBrush = if (animatedProgress >= 1f) {
                        SolidColor(accentColor)
                    } else {
                        Brush.linearGradient(
                            colors = listOf(primaryColor, accentColor),
                            start = topLeft,
                            end = Offset(topLeft.x + diameter, topLeft.y + diameter)
                        )
                    }

                    drawArc(
                        brush = arcBrush,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 4. Glowing pip bead at the leading tip of progress
                    if (animatedProgress in 0.015f..0.995f) {
                        val tipAngleDeg = -90f + sweepAngle
                        val tipRad = Math.toRadians(tipAngleDeg.toDouble())
                        val ringRadius = diameter / 2f
                        val tipX = centerOffset.x + ringRadius * cos(tipRad).toFloat()
                        val tipY = centerOffset.y + ringRadius * sin(tipRad).toFloat()

                        // Outer soft glow
                        drawCircle(
                            color = accentColor.copy(alpha = 0.35f),
                            radius = strokeWidth * 0.75f,
                            center = Offset(tipX, tipY)
                        )
                        // Inner bright bead
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = strokeWidth * 0.32f,
                            center = Offset(tipX, tipY)
                        )
                    }
                }
            }

            // Counter & Chronometer inside ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = days,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { height -> height } + fadeIn())
                                .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically { height -> -height } + fadeIn())
                                .togetherWith(slideOutVertically { height -> height } + fadeOut())
                        }
                    },
                    label = "dayCountText"
                ) { count ->
                    val countFontSize = when {
                        count >= 1000 -> 36.sp
                        count >= 100 -> 42.sp
                        else -> 48.sp
                    }
                    val countLineHeight = when {
                        count >= 1000 -> 34.sp
                        count >= 100 -> 40.sp
                        else -> 46.sp
                    }
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = countFontSize,
                            lineHeight = countLineHeight,
                            fontFamily = BarlowCondensed,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = if (days == 1) "DAY" else stringResource(R.string.days_label).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = BarlowCondensed,
                        letterSpacing = 2.4.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )

                if (startedAtMillis > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ElapsedTicker(
                        startedAtMillis = startedAtMillis,
                        isDayZero = days == 0
                    )
                }
            }
        }
    }
}

@Composable
private fun ElapsedTicker(
    startedAtMillis: Long,
    isDayZero: Boolean,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isCompactMode by remember { mutableStateOf(true) }

    LaunchedEffect(startedAtMillis) {
        while (isActive) {
            nowMillis = System.currentTimeMillis()
            val delayMs = 1000L - (nowMillis % 1000L)
            delay(delayMs.coerceAtLeast(50L))
        }
    }

    val elapsedSeconds = ((nowMillis - startedAtMillis) / 1000).coerceAtLeast(0L)
    val hours = (elapsedSeconds / 3600) % 24
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60

    val text = if (isCompactMode) {
        if (isDayZero) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "+%02d:%02d:%02d", hours, minutes, seconds)
        }
    } else {
        if (isDayZero) {
            String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "+ %02dh %02dm %02ds", hours, minutes, seconds)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "tickerPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val haptic = LocalHapticFeedback.current
    val accentColor = LocalRankColors.current.accent
    val primaryColor = MaterialTheme.colorScheme.primary

    val indicatorColor = if (isDayZero) primaryColor else accentColor
    val backgroundColor = if (isDayZero) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val borderColor = if (isDayZero) {
        primaryColor.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }
    val textColor = if (isDayZero) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(width = 1.dp, color = borderColor),
        modifier = modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isCompactMode = !isCompactMode
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
        ) {
            // Pulsing live indicator dot
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(indicatorColor.copy(alpha = pulseAlpha))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.4.sp
                ),
                color = textColor
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "DayCounter Light")
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "DayCounter Dark")
@Composable
private fun DayCounterPreview() {
    com.example.steadfast.ui.theme.SteadfastTheme {
        DayCounter(
            days = 12,
            progressToNext = 0.65f,
            startedAtMillis = System.currentTimeMillis() - (4 * 3600 + 23 * 60 + 12) * 1000L
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "DayCounter Day 0")
@Composable
private fun DayCounterDayZeroPreview() {
    com.example.steadfast.ui.theme.SteadfastTheme {
        DayCounter(
            days = 0,
            progressToNext = 0.20f,
            startedAtMillis = System.currentTimeMillis() - (3 * 3600 + 15 * 60 + 42) * 1000L
        )
    }
}
