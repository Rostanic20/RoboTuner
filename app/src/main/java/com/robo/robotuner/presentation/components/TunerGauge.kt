package com.robo.robotuner.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robo.robotuner.presentation.tuner.TunerReading
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.DarkCard
import com.robo.robotuner.ui.theme.TextMuted
import kotlin.math.cos
import kotlin.math.sin

private const val RANGE_CENTS = 50.0
private const val HALF_SWEEP = 120.0
private const val TOP = 270.0
private val LABELS = listOf(-50, -25, 0, 25, 50)

@Composable
fun TunerGauge(
    reading: TunerReading?,
    toleranceCents: Double,
    modifier: Modifier = Modifier
) {
    val cents = reading?.cents?.coerceIn(-RANGE_CENTS, RANGE_CENTS) ?: 0.0
    val animatedCents by animateFloatAsState(
        targetValue = cents.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "cents"
    )
    val color by animateColorAsState(
        targetValue = reading?.let { tuningColor(it) } ?: TextMuted,
        animationSpec = tween(150),
        label = "gaugeColor"
    )
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.035f
        val radius = size.minDimension / 2 - stroke
        val center = Offset(size.width / 2, size.height / 2)

        drawArcSegment(center, radius, -RANGE_CENTS, RANGE_CENTS, DarkCard, stroke)
        drawArcSegment(
            center, radius,
            -toleranceCents, toleranceCents,
            AccentGreen.copy(alpha = 0.3f), stroke
        )
        drawScale(center, radius - stroke * 1.6f, textMeasurer)

        if (reading != null) {
            drawArcSegment(center, radius, 0.0, animatedCents.toDouble(), color.copy(alpha = 0.85f), stroke)
            val knob = pointOnArc(center, radius, animatedCents.toDouble())
            drawCircle(color.copy(alpha = 0.25f), radius = stroke * 1.6f, center = knob)
            drawCircle(color, radius = stroke * 0.95f, center = knob)
        }
    }
}

private fun angleFor(cents: Double) = TOP + cents / RANGE_CENTS * HALF_SWEEP

private fun pointOnArc(center: Offset, radius: Float, cents: Double): Offset {
    val angle = Math.toRadians(angleFor(cents))
    return Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
}

private fun DrawScope.drawArcSegment(
    center: Offset,
    radius: Float,
    fromCents: Double,
    toCents: Double,
    color: Color,
    stroke: Float
) {
    val start = angleFor(minOf(fromCents, toCents))
    val sweep = angleFor(maxOf(fromCents, toCents)) - start
    if (sweep <= 0.0) return
    drawArc(
        color = color,
        startAngle = start.toFloat(),
        sweepAngle = sweep.toFloat(),
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawScale(center: Offset, radius: Float, textMeasurer: TextMeasurer) {
    for (cents in -50..50 step 5) {
        val major = cents % 25 == 0
        val length = if (major) radius * 0.09f else radius * 0.045f
        val outer = pointOnArc(center, radius, cents.toDouble())
        val inner = pointOnArc(center, radius - length, cents.toDouble())
        drawLine(
            color = if (cents == 0) AccentGreen else TextMuted.copy(alpha = if (major) 1f else 0.5f),
            start = inner,
            end = outer,
            strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
    val labelStyle = TextStyle(color = TextMuted, fontSize = (radius * 0.075f / density / fontScale).sp)
    for (cents in LABELS) {
        val text = if (cents > 0) "+$cents" else "$cents"
        val layout = textMeasurer.measure(text, labelStyle)
        val anchor = pointOnArc(center, radius * 0.8f, cents.toDouble())
        drawText(layout, topLeft = Offset(anchor.x - layout.size.width / 2f, anchor.y - layout.size.height / 2f))
    }
}
